/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.xml.SecureXMLFactoryProviderUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TreeMapBuilder;
import com.liferay.portal.kernel.util.Validator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.crypto.Cipher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.RelatedCryptoMaterialType;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

/**
 * @author Manuele Castro
 */
public class CryptoCallScanner {

	public static boolean isJDKPropertyKey(String propertyKey) {
		return _jdkPropertyKeys.containsValue(propertyKey);
	}

	public CryptoCallScanner(boolean fipsEnabled) {
		_fipsEnabled = fipsEnabled;
	}

	public List<CryptoCall> getCryptoCalls() throws IOException {
		_follow();

		List<CryptoCall> cryptoCalls = new ArrayList<>();

		for (ScannedCryptoCall scannedCryptoCall : _scannedCryptoCalls) {
			cryptoCalls.addAll(_toCryptoCalls(scannedCryptoCall));
		}

		return cryptoCalls;
	}

	public void scan(File file) throws IOException {
		_files.add(file);

		_visit(
			file, this::_isScannedFileName,
			(bundleSymbolicName, featureName, name, bytes) -> {
				if (name.endsWith(".class")) {
					_scan(bundleSymbolicName, featureName, bytes);
				}
				else {
					_scanSpringXML(bytes);
				}
			});
	}

	public void scan(
			String bundleSymbolicName, String featureName,
			InputStream inputStream)
		throws IOException {

		byte[] bytes = inputStream.readAllBytes();

		_scannedClassBytes.add(bytes);

		_scan(bundleSymbolicName, featureName, bytes);
	}

	private void _addLibraryCryptoCall(
		String algorithm, String bundleSymbolicName, String className,
		List<CryptoFunction> cryptoFunctions, String curve, String featureName,
		String jcaClassName, int keySize, String library, String note,
		String unresolvedReason) {

		CryptoCall cryptoCall = new CryptoCall(
			bundleSymbolicName, className, featureName, jcaClassName);

		cryptoCall.setCryptoFunctions(cryptoFunctions);
		cryptoCall.setCurve(curve);
		cryptoCall.setLibrary(library);
		cryptoCall.setNote(note);

		Origin algorithmOrigin = new Origin();

		if (algorithm != null) {
			algorithmOrigin.addValue(algorithm);
		}

		if (unresolvedReason != null) {
			algorithmOrigin.addUnresolvedReason(unresolvedReason);
		}

		Origin keySizeOrigin = new Origin();

		if (keySize > 0) {
			keySizeOrigin.addValue(String.valueOf(keySize));
		}

		_scannedCryptoCalls.add(
			new ScannedCryptoCall(algorithmOrigin, cryptoCall, keySizeOrigin));
	}

	private void _addOpenSAMLCryptoCalls(
		String bundleSymbolicName, String className, Set<String> excludedURIs,
		String featureName) {

		for (Map.Entry<String, LibraryAlgorithm> entry :
				_openSAMLAlgorithms.entrySet()) {

			if (excludedURIs.contains(entry.getKey())) {
				continue;
			}

			LibraryAlgorithm libraryAlgorithm = entry.getValue();

			CryptoFunction cryptoFunction = _cryptoFunctions.get(
				libraryAlgorithm.getJCAClassName());

			_addLibraryCryptoCall(
				libraryAlgorithm.getAlgorithm(), bundleSymbolicName, className,
				_libraryCryptoFunctions.getOrDefault(
					libraryAlgorithm.getJCAClassName(),
					Collections.singletonList(cryptoFunction)),
				null, featureName, libraryAlgorithm.getJCAClassName(),
				libraryAlgorithm.getKeySize(), "OpenSAML",
				StringBundler.concat(
					"One of OpenSAML's default algorithms, ", entry.getKey(),
					". The blacklisted.algorithms setting of the SAML ",
					"configuration can exclude it."),
				null);
		}
	}

	private void _addVerifiedJWSCryptoCalls(
		String bundleSymbolicName, String className, String featureName) {

		if (!_fipsEnabled) {
			_addLibraryCryptoCall(
				null, bundleSymbolicName, className,
				Collections.singletonList(CryptoFunction.VERIFY), null,
				featureName, "Signature", 0, "nimbus-jose-jwt", null,
				"the algorithm in the header of each JSON Web Token, which " +
					"the token's issuer chooses");

			return;
		}

		for (String jwsAlgorithm : FIPSModeLimitUtil.getJWSAlgorithms()) {
			for (LibraryAlgorithm libraryAlgorithm :
					_jwsAlgorithms.getOrDefault(
						jwsAlgorithm, Collections.emptyList())) {

				_addLibraryCryptoCall(
					libraryAlgorithm.getAlgorithm(), bundleSymbolicName,
					className, Collections.singletonList(CryptoFunction.VERIFY),
					libraryAlgorithm.getCurve(), featureName,
					libraryAlgorithm.getJCAClassName(), 0, "nimbus-jose-jwt",
					StringBundler.concat(
						"The JWS algorithm ", jwsAlgorithm,
						", which FIPS mode allows for the tokens this module ",
						"verifies"),
					null);
			}
		}
	}

	private Frame<SourceValue>[] _analyze(
			ClassNode classNode, MethodNode methodNode)
		throws AnalyzerException {

		_foldFIPSEnabled(methodNode.instructions);

		Analyzer<SourceValue> analyzer = new Analyzer<>(
			new OriginInterpreter(
				_getMethodKey(classNode.name, methodNode.name, methodNode.desc),
				methodNode));

		return analyzer.analyze(classNode.name, methodNode);
	}

	private void _collectReferences(
		Origin origin, Set<String> references, Set<String> visitedReferences) {

		List<String> originReferences = new ArrayList<>(origin.getReferences());

		originReferences.addAll(origin.getDerivedReferences());

		for (String reference : originReferences) {
			if (!visitedReferences.add(reference)) {
				continue;
			}

			Origin referenceOrigin = _getReferenceOrigin(reference);

			if (referenceOrigin == null) {
				if (reference.startsWith("com.liferay.")) {
					references.add(reference);
				}

				continue;
			}

			_collectReferences(referenceOrigin, references, visitedReferences);
		}
	}

	private boolean _containsAny(String content, Collection<String> needles) {
		for (String needle : needles) {
			if (content.contains(needle)) {
				return true;
			}
		}

		return false;
	}

	private void _foldFIPSEnabled(InsnList insnList) {
		for (AbstractInsnNode abstractInsnNode : insnList.toArray()) {
			if (!_isFIPSEnabled(abstractInsnNode)) {
				continue;
			}

			AbstractInsnNode nextAbstractInsnNode = abstractInsnNode.getNext();

			while ((nextAbstractInsnNode != null) &&
				   (nextAbstractInsnNode.getOpcode() == -1)) {

				nextAbstractInsnNode = nextAbstractInsnNode.getNext();
			}

			if ((nextAbstractInsnNode == null) ||
				((nextAbstractInsnNode.getOpcode() != Opcodes.IFEQ) &&
				 (nextAbstractInsnNode.getOpcode() != Opcodes.IFNE))) {

				continue;
			}

			JumpInsnNode jumpInsnNode = (JumpInsnNode)nextAbstractInsnNode;

			insnList.remove(abstractInsnNode);

			if ((jumpInsnNode.getOpcode() == Opcodes.IFEQ) != _fipsEnabled) {
				insnList.set(
					jumpInsnNode,
					new JumpInsnNode(Opcodes.GOTO, jumpInsnNode.label));
			}
			else {
				insnList.remove(jumpInsnNode);
			}
		}
	}

	private void _follow() throws IOException {
		for (int i = 0; i < _FOLLOW_PASS_COUNT; i++) {
			Set<String> references = new TreeSet<>();

			for (ScannedCryptoCall scannedCryptoCall : _scannedCryptoCalls) {
				_collectReferences(
					scannedCryptoCall.getAlgorithmOrigin(), references,
					new HashSet<>());
				_collectReferences(
					scannedCryptoCall.getKeySizeOrigin(), references,
					new HashSet<>());
			}

			references.removeAll(_followedReferences);

			if (references.isEmpty()) {
				return;
			}

			_followedReferences.addAll(references);

			_follow(references);
		}
	}

	private void _follow(Set<String> references) throws IOException {
		Set<String> needles = new HashSet<>();
		Map<String, List<String>> parameterReferences = new HashMap<>();

		for (String reference : references) {
			int index = reference.indexOf(CharPool.POUND);

			if (index == -1) {
				String className = _getReferenceClassName(reference);

				needles.add(StringUtil.replace(className, '.', '/'));

				for (String superTypeName :
						_getSuperTypeNames(
							StringUtil.replace(className, '.', '/'))) {

					if (!superTypeName.startsWith("com/liferay/")) {
						continue;
					}

					needles.add(superTypeName);
				}

				continue;
			}

			String methodName = _getReferenceMethodName(reference);

			if (Objects.equals(methodName, "<init>")) {
				needles.add(
					StringUtil.replace(
						_getReferenceClassName(reference), '.', '/'));
			}
			else {
				needles.add(methodName);
			}

			List<String> methodParameterReferences =
				parameterReferences.computeIfAbsent(
					methodName, key -> new ArrayList<>());

			methodParameterReferences.add(reference);
		}

		for (File file : _files) {
			_visit(
				file, this::_isFollowedFileName,
				(bundleSymbolicName, featureName, name, bytes) -> {
					if (name.endsWith(".class")) {
						_followClass(bytes, needles, parameterReferences);
					}
					else {
						_followJSP(
							new String(bytes, StandardCharsets.UTF_8),
							parameterReferences);
					}
				});
		}

		for (byte[] bytes : _scannedClassBytes) {
			_followClass(bytes, needles, parameterReferences);
		}
	}

	private void _followClass(
		byte[] bytes, Set<String> needles,
		Map<String, List<String>> parameterReferences) {

		String classFileContent = new String(
			bytes, StandardCharsets.ISO_8859_1);

		if (!_containsAny(classFileContent, needles)) {
			return;
		}

		ClassNode classNode = new ClassNode();

		ClassReader classReader = new ClassReader(bytes);

		classReader.accept(
			classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		_recordConfigurationAttributes(classNode);

		for (MethodNode methodNode : classNode.methods) {
			if (methodNode.instructions.size() == 0) {
				continue;
			}

			Frame<SourceValue>[] frames = null;

			try {
				frames = _analyze(classNode, methodNode);
			}
			catch (AnalyzerException analyzerException) {
				if (_log.isDebugEnabled()) {
					_log.debug(analyzerException);
				}

				continue;
			}

			_scanAssignments(classNode, frames, methodNode);

			InsnList insnList = methodNode.instructions;

			for (AbstractInsnNode abstractInsnNode : insnList) {
				if (!(abstractInsnNode instanceof MethodInsnNode) ||
					(frames[insnList.indexOf(abstractInsnNode)] == null)) {

					continue;
				}

				MethodInsnNode methodInsnNode =
					(MethodInsnNode)abstractInsnNode;

				List<String> methodParameterReferences =
					parameterReferences.get(methodInsnNode.name);

				if (methodParameterReferences == null) {
					continue;
				}

				for (String reference : methodParameterReferences) {
					_followParameterReference(
						frames, insnList, methodInsnNode, reference);
				}
			}
		}
	}

	private void _followJSP(
		String content, Map<String, List<String>> parameterReferences) {

		for (Map.Entry<String, List<String>> entry :
				parameterReferences.entrySet()) {

			Pattern pattern = Pattern.compile(
				"\\." + Pattern.quote(entry.getKey()) + "\\(");

			Matcher matcher = pattern.matcher(content);

			while (matcher.find()) {
				List<String> arguments = _getJSPArguments(
					content, matcher.end());

				for (String reference : entry.getValue()) {
					if (reference.endsWith(_SUFFIX_KEY_ALGORITHM)) {
						continue;
					}

					int index = GetterUtil.getInteger(
						reference.substring(
							reference.indexOf(CharPool.POUND) + 1));

					if (index >= arguments.size()) {
						continue;
					}

					Matcher literalMatcher = _jspLiteralPattern.matcher(
						arguments.get(index));

					if (!literalMatcher.matches()) {
						continue;
					}

					Origin origin = new Origin();

					origin.addValue(literalMatcher.group(1));

					_track(reference, origin);
				}
			}
		}
	}

	private void _followParameterReference(
		Frame<SourceValue>[] frames, InsnList insnList,
		MethodInsnNode methodInsnNode, String reference) {

		String methodKey = reference.substring(
			0, reference.indexOf(CharPool.POUND));

		String desc = methodKey.substring(
			methodKey.indexOf(CharPool.OPEN_PARENTHESIS));

		if (!Objects.equals(methodInsnNode.desc, desc) ||
			!_isCompatible(
				methodInsnNode.owner,
				StringUtil.replace(
					_getReferenceClassName(reference), '.', '/'))) {

			return;
		}

		String parameter = reference.substring(
			reference.indexOf(CharPool.POUND) + 1);

		boolean keyAlgorithm = false;

		if (parameter.endsWith(_SUFFIX_KEY_ALGORITHM)) {
			keyAlgorithm = true;

			parameter = parameter.substring(
				0, parameter.length() - _SUFFIX_KEY_ALGORITHM.length());
		}

		int count = Type.getArgumentCount(methodInsnNode.desc);
		int index = GetterUtil.getInteger(parameter);

		if (index >= count) {
			return;
		}

		if (keyAlgorithm) {
			_track(
				reference,
				_getKeyAlgorithmOrigin(
					frames, insnList,
					_getArgument(
						frames[insnList.indexOf(methodInsnNode)], index, count),
					new HashSet<>()));
		}
		else {
			_track(
				reference,
				_getArgumentOrigin(
					frames, insnList, methodInsnNode, index, count));
		}
	}

	private Origin _getAlgorithmOrigin(
		Frame<SourceValue>[] frames, InsnList insnList,
		MethodInsnNode methodInsnNode) {

		Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);

		if (Objects.equals(methodInsnNode.owner, _CLASS_NAME_DIGESTER_UTIL) &&
			((argumentTypes.length < 2) ||
			 !Objects.equals(argumentTypes[0], Type.getType(String.class)))) {

			Origin origin = new Origin();

			origin.addReference(
				_getMethodKey(
					_CLASS_NAME_DIGESTER_UTIL, "_getDefaultAlgorithm",
					"()Ljava/lang/String;"));

			return origin;
		}

		int index = 0;

		if (Objects.equals(methodInsnNode.owner, _CLASS_NAME_SECRET_KEY_SPEC)) {
			index = argumentTypes.length - 1;
		}

		return _getArgumentOrigin(
			frames, insnList, methodInsnNode, index, argumentTypes.length);
	}

	private List<AnnotationNode> _getAnnotationNodes(
		List<AnnotationNode> invisibleAnnotationNodes,
		List<AnnotationNode> visibleAnnotationNodes) {

		List<AnnotationNode> annotationNodes = new ArrayList<>();

		if (invisibleAnnotationNodes != null) {
			annotationNodes.addAll(invisibleAnnotationNodes);
		}

		if (visibleAnnotationNodes != null) {
			annotationNodes.addAll(visibleAnnotationNodes);
		}

		return annotationNodes;
	}

	private String _getAnnotationValue(
		List<AnnotationNode> annotationNodes, String desc, String name) {

		for (AnnotationNode annotationNode : annotationNodes) {
			if (!Objects.equals(annotationNode.desc, desc)) {
				continue;
			}

			if (annotationNode.values == null) {
				return StringPool.BLANK;
			}

			for (int i = 0; i < annotationNode.values.size(); i += 2) {
				if (Objects.equals(annotationNode.values.get(i), name)) {
					return String.valueOf(annotationNode.values.get(i + 1));
				}
			}

			return StringPool.BLANK;
		}

		return null;
	}

	private SourceValue _getArgument(
		Frame<SourceValue> frame, int index, int count) {

		return frame.getStack(frame.getStackSize() - count + index);
	}

	private Origin _getArgumentOrigin(
		Frame<SourceValue>[] frames, InsnList insnList,
		AbstractInsnNode abstractInsnNode, int index, int count) {

		return _getOrigin(
			frames, insnList,
			_getArgument(
				frames[insnList.indexOf(abstractInsnNode)], index, count),
			new HashSet<>());
	}

	private String _getBundleSymbolicName(String fileName, Manifest manifest) {
		if (manifest != null) {
			Attributes attributes = manifest.getMainAttributes();

			String bundleSymbolicName = attributes.getValue(
				"Bundle-SymbolicName");

			if (Validator.isNotNull(bundleSymbolicName)) {
				int index = bundleSymbolicName.indexOf(CharPool.SEMICOLON);

				if (index != -1) {
					bundleSymbolicName = bundleSymbolicName.substring(0, index);
				}

				return StringUtil.trim(bundleSymbolicName);
			}
		}

		return _getFileName(fileName);
	}

	private String _getClassName(String internalName) {
		String className = StringUtil.replace(
			internalName, CharPool.SLASH, CharPool.PERIOD);

		int index = className.indexOf(CharPool.DOLLAR);

		if (index != -1) {
			className = className.substring(0, index);
		}

		return className;
	}

	private Origin _getComputedOrigin(
		Frame<SourceValue>[] frames, InsnList insnList,
		List<SourceValue> sourceValues,
		Set<AbstractInsnNode> visitedAbstractInsnNodes) {

		Origin origin = new Origin();

		origin.setComputed(true);

		for (SourceValue sourceValue : sourceValues) {
			Origin inputOrigin = _getOrigin(
				frames, insnList, sourceValue, visitedAbstractInsnNodes);

			for (String propertyKey : inputOrigin.getPropertyKeys()) {
				origin.addPropertyKey(propertyKey);
			}

			for (String reference : inputOrigin.getReferences()) {
				origin.addDerivedReference(reference);
			}

			for (String reference : inputOrigin.getDerivedReferences()) {
				origin.addDerivedReference(reference);
			}

			for (String unresolvedReason : inputOrigin.getUnresolvedReasons()) {
				origin.addUnresolvedReason(unresolvedReason);
			}
		}

		return origin;
	}

	private List<CryptoFunction> _getCryptoFunctions(
		Frame<SourceValue>[] frames, InsnList insnList, String jcaClassName,
		MethodInsnNode methodInsnNode) {

		CryptoFunction cryptoFunction = _cryptoFunctions.get(jcaClassName);

		if (cryptoFunction != null) {
			return Collections.singletonList(cryptoFunction);
		}

		Set<CryptoFunction> cryptoFunctions = new TreeSet<>();

		for (MethodInsnNode linkedMethodInsnNode :
				_getLinkedMethodInsnNodes(frames, insnList, methodInsnNode)) {

			if (Objects.equals(linkedMethodInsnNode.name, "initSign")) {
				cryptoFunctions.add(CryptoFunction.SIGN);
			}
			else if (Objects.equals(linkedMethodInsnNode.name, "initVerify")) {
				cryptoFunctions.add(CryptoFunction.VERIFY);
			}
			else if (Objects.equals(linkedMethodInsnNode.name, "init") &&
					 linkedMethodInsnNode.desc.startsWith("(I")) {

				Origin origin = _getArgumentOrigin(
					frames, insnList, linkedMethodInsnNode, 0,
					Type.getArgumentCount(linkedMethodInsnNode.desc));

				for (String value : origin.getValues()) {
					cryptoFunction = _cipherCryptoFunctions.get(value);

					if (cryptoFunction == null) {
						continue;
					}

					cryptoFunctions.add(cryptoFunction);
				}
			}
		}

		if (cryptoFunctions.isEmpty()) {
			return Collections.singletonList(CryptoFunction.UNKNOWN);
		}

		return new ArrayList<>(cryptoFunctions);
	}

	private List<MethodInsnNode> _getCryptoMethodInsnNodes(
		ClassNode classNode, MethodNode methodNode) {

		List<MethodInsnNode> methodInsnNodes = new ArrayList<>();

		if (Objects.equals(classNode.name, _CLASS_NAME_DIGESTER_UTIL)) {
			return methodInsnNodes;
		}

		for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
			if (!(abstractInsnNode instanceof MethodInsnNode)) {
				continue;
			}

			MethodInsnNode methodInsnNode = (MethodInsnNode)abstractInsnNode;

			if (_getJCAClassName(methodInsnNode) == null) {
				continue;
			}

			methodInsnNodes.add(methodInsnNode);
		}

		return methodInsnNodes;
	}

	private String _getFeatureName(String fileName, Manifest manifest) {
		if (manifest != null) {
			Attributes attributes = manifest.getMainAttributes();

			String bundleName = attributes.getValue("Bundle-Name");

			if (Validator.isNotNull(bundleName)) {
				return bundleName;
			}
		}

		return _getFileName(fileName);
	}

	private String _getFieldKey(FieldInsnNode fieldInsnNode) {
		return _getKeyName(fieldInsnNode.owner) + StringPool.PERIOD +
			fieldInsnNode.name;
	}

	private String _getFileName(String fileName) {
		String name = fileName.substring(
			fileName.lastIndexOf(CharPool.SLASH) + 1);

		int index = name.lastIndexOf(CharPool.PERIOD);

		if (index > 0) {
			name = name.substring(0, index);
		}

		return name;
	}

	private Origin _getImplementationReturnValueOrigin(String reference) {
		String className = _getReferenceClassName(reference);

		String internalName = StringUtil.replace(className, '.', '/');
		String methodSignature = reference.substring(className.length());

		Origin origin = null;

		for (String subtypeName : _getSubtypeNames(internalName)) {
			Origin subtypeOrigin = _referenceOrigins.get(
				_getKeyName(subtypeName) + methodSignature);

			if (subtypeOrigin == null) {
				continue;
			}

			if (origin == null) {
				origin = new Origin();
			}

			origin.addAll(subtypeOrigin);
		}

		return origin;
	}

	private Origin _getInheritedReturnValueOrigin(String reference) {
		String className = _getReferenceClassName(reference);

		String methodSignature = reference.substring(className.length());

		for (String superTypeName :
				_getSuperTypeNames(StringUtil.replace(className, '.', '/'))) {

			String superTypeReference =
				_getKeyName(superTypeName) + methodSignature;

			Origin origin = _referenceOrigins.get(superTypeReference);

			if (origin != null) {
				return origin;
			}

			String settingKey = _configurationSettingKeys.get(
				superTypeReference.substring(
					0, superTypeReference.indexOf(CharPool.OPEN_PARENTHESIS)));

			if (settingKey != null) {
				origin = new Origin();

				origin.addPropertyKey(settingKey);

				return origin;
			}
		}

		return null;
	}

	private MethodInsnNode _getInitMethodInsnNode(
		Frame<SourceValue>[] frames, InsnList insnList,
		TypeInsnNode typeInsnNode) {

		for (AbstractInsnNode abstractInsnNode : insnList) {
			if (abstractInsnNode.getOpcode() != Opcodes.INVOKESPECIAL) {
				continue;
			}

			MethodInsnNode methodInsnNode = (MethodInsnNode)abstractInsnNode;

			Frame<SourceValue> frame = frames[insnList.indexOf(methodInsnNode)];

			if ((frame == null) ||
				!Objects.equals(methodInsnNode.owner, typeInsnNode.desc) ||
				!Objects.equals(methodInsnNode.name, "<init>")) {

				continue;
			}

			SourceValue sourceValue = _getReceiver(
				frame, Type.getArgumentCount(methodInsnNode.desc));

			if (sourceValue.insns.contains(typeInsnNode)) {
				return methodInsnNode;
			}
		}

		return null;
	}

	private String _getJCAClassName(MethodInsnNode methodInsnNode) {
		if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC) {
			if (Objects.equals(
					methodInsnNode.owner, _CLASS_NAME_DIGESTER_UTIL) &&
				methodInsnNode.name.startsWith("digest")) {

				return "MessageDigest";
			}

			if (Objects.equals(methodInsnNode.name, "getInstance") &&
				methodInsnNode.desc.startsWith("(Ljava/lang/String;")) {

				return _jcaClassNames.get(methodInsnNode.owner);
			}

			return null;
		}

		if ((methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL) &&
			Objects.equals(methodInsnNode.owner, _CLASS_NAME_SECRET_KEY_SPEC) &&
			Objects.equals(methodInsnNode.name, "<init>") &&
			methodInsnNode.desc.endsWith("Ljava/lang/String;)V")) {

			return "SecretKeySpec";
		}

		return null;
	}

	private List<String> _getJSPArguments(String content, int start) {
		List<String> arguments = new ArrayList<>();

		int depth = 0;
		boolean quoted = false;

		StringBundler sb = new StringBundler();

		for (int i = start; i < content.length(); i++) {
			char c = content.charAt(i);

			if (quoted) {
				sb.append(c);

				if (c == CharPool.QUOTE) {
					quoted = false;
				}

				continue;
			}

			if (c == CharPool.QUOTE) {
				quoted = true;
			}
			else if (c == CharPool.OPEN_PARENTHESIS) {
				depth++;
			}
			else if (c == CharPool.CLOSE_PARENTHESIS) {
				if (depth == 0) {
					arguments.add(StringUtil.trim(sb.toString()));

					return arguments;
				}

				depth--;
			}
			else if ((c == CharPool.COMMA) && (depth == 0)) {
				arguments.add(StringUtil.trim(sb.toString()));

				sb.setIndex(0);

				continue;
			}

			sb.append(c);
		}

		return arguments;
	}

	private Origin _getKeyAlgorithmOrigin(
		Frame<SourceValue>[] frames, InsnList insnList, SourceValue sourceValue,
		Set<AbstractInsnNode> visitedAbstractInsnNodes) {

		Origin origin = new Origin();

		for (AbstractInsnNode abstractInsnNode : sourceValue.insns) {
			if (!visitedAbstractInsnNodes.add(abstractInsnNode)) {
				continue;
			}

			int opcode = abstractInsnNode.getOpcode();

			if (abstractInsnNode instanceof ParameterInsnNode) {
				ParameterInsnNode parameterInsnNode =
					(ParameterInsnNode)abstractInsnNode;

				origin.addReference(
					parameterInsnNode.getParameterKey() +
						_SUFFIX_KEY_ALGORITHM);
			}
			else if (opcode == Opcodes.NEW) {
				TypeInsnNode typeInsnNode = (TypeInsnNode)abstractInsnNode;

				MethodInsnNode methodInsnNode = _getInitMethodInsnNode(
					frames, insnList, typeInsnNode);

				if ((methodInsnNode != null) &&
					Objects.equals(
						typeInsnNode.desc, _CLASS_NAME_SECRET_KEY_SPEC) &&
					methodInsnNode.desc.endsWith("Ljava/lang/String;)V")) {

					origin.addAll(
						_getAlgorithmOrigin(frames, insnList, methodInsnNode));
				}
				else {
					origin.addUnresolvedReason(
						"a key that the code builds as a " +
							_getKeyName(typeInsnNode.desc));
				}
			}
			else if (opcode == Opcodes.CHECKCAST) {
				origin.addAll(
					_getKeyAlgorithmOrigin(
						frames, insnList,
						_getArgument(
							frames[insnList.indexOf(abstractInsnNode)], 0, 1),
						visitedAbstractInsnNodes));
			}
			else if (abstractInsnNode instanceof MethodInsnNode) {
				MethodInsnNode methodInsnNode =
					(MethodInsnNode)abstractInsnNode;

				if (Objects.equals(
						methodInsnNode.owner, "javax/crypto/KeyGenerator") &&
					Objects.equals(methodInsnNode.name, "generateKey")) {

					Frame<SourceValue> frame =
						frames[insnList.indexOf(methodInsnNode)];

					SourceValue receiverSourceValue = _getReceiver(frame, 0);

					for (AbstractInsnNode receiverAbstractInsnNode :
							receiverSourceValue.insns) {

						if (!(receiverAbstractInsnNode instanceof
								MethodInsnNode)) {

							continue;
						}

						MethodInsnNode receiverMethodInsnNode =
							(MethodInsnNode)receiverAbstractInsnNode;

						String jcaClassName = _getJCAClassName(
							receiverMethodInsnNode);

						if (jcaClassName == null) {
							continue;
						}

						origin.addAll(
							_getAlgorithmOrigin(
								frames, insnList, receiverMethodInsnNode));
					}
				}
				else {
					String methodKey = _getMethodKey(
						methodInsnNode.owner, methodInsnNode.name,
						methodInsnNode.desc);

					origin.addReference(methodKey + _SUFFIX_KEY_ALGORITHM);
				}
			}
			else if ((opcode == Opcodes.GETFIELD) ||
					 (opcode == Opcodes.GETSTATIC)) {

				origin.addReference(
					_getFieldKey((FieldInsnNode)abstractInsnNode) +
						_SUFFIX_KEY_ALGORITHM);
			}
			else if (opcode != Opcodes.ACONST_NULL) {
				origin.addUnresolvedReason("a key the code computes");
			}
		}

		return origin;
	}

	private String _getKeyName(String internalName) {
		return StringUtil.replace(
			internalName, CharPool.SLASH, CharPool.PERIOD);
	}

	private Origin _getKeySizeOrigin(
		Frame<SourceValue>[] frames, InsnList insnList,
		MethodInsnNode methodInsnNode) {

		Origin origin = new Origin();

		String methodName = _keySizeMethodNames.get(methodInsnNode.owner);

		if (methodName == null) {
			return origin;
		}

		for (MethodInsnNode linkedMethodInsnNode :
				_getLinkedMethodInsnNodes(frames, insnList, methodInsnNode)) {

			if (!Objects.equals(linkedMethodInsnNode.name, methodName) ||
				!linkedMethodInsnNode.desc.startsWith("(I")) {

				continue;
			}

			origin.addAll(
				_getArgumentOrigin(
					frames, insnList, linkedMethodInsnNode, 0,
					Type.getArgumentCount(linkedMethodInsnNode.desc)));
		}

		return origin;
	}

	private List<MethodInsnNode> _getLinkedMethodInsnNodes(
		Frame<SourceValue>[] frames, InsnList insnList,
		MethodInsnNode methodInsnNode) {

		List<MethodInsnNode> linkedMethodInsnNodes = new ArrayList<>();

		for (AbstractInsnNode abstractInsnNode : insnList) {
			if (abstractInsnNode.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			MethodInsnNode curMethodInsnNode = (MethodInsnNode)abstractInsnNode;

			Frame<SourceValue> frame =
				frames[insnList.indexOf(curMethodInsnNode)];

			if ((frame == null) ||
				!Objects.equals(
					curMethodInsnNode.owner, methodInsnNode.owner)) {

				continue;
			}

			SourceValue sourceValue = _getReceiver(
				frame, Type.getArgumentCount(curMethodInsnNode.desc));

			if (!sourceValue.insns.contains(methodInsnNode)) {
				continue;
			}

			linkedMethodInsnNodes.add(curMethodInsnNode);
		}

		return linkedMethodInsnNodes;
	}

	private String _getMethodKey(
		String internalName, String name, String desc) {

		return StringBundler.concat(
			_getKeyName(internalName), StringPool.PERIOD, name, desc);
	}

	private Origin _getOrigin(
		Frame<SourceValue>[] frames, InsnList insnList,
		AbstractInsnNode abstractInsnNode,
		Set<AbstractInsnNode> visitedAbstractInsnNodes) {

		Origin origin = new Origin();

		int opcode = abstractInsnNode.getOpcode();

		if (abstractInsnNode instanceof ParameterInsnNode) {
			ParameterInsnNode parameterInsnNode =
				(ParameterInsnNode)abstractInsnNode;

			origin.addReference(parameterInsnNode.getParameterKey());
		}
		else if (abstractInsnNode instanceof LdcInsnNode) {
			LdcInsnNode ldcInsnNode = (LdcInsnNode)abstractInsnNode;

			origin.addValue(String.valueOf(ldcInsnNode.cst));
		}
		else if ((opcode >= Opcodes.ICONST_M1) &&
				 (opcode <= Opcodes.ICONST_5)) {

			origin.addValue(String.valueOf(opcode - Opcodes.ICONST_0));
		}
		else if ((opcode == Opcodes.BIPUSH) || (opcode == Opcodes.SIPUSH)) {
			IntInsnNode intInsnNode = (IntInsnNode)abstractInsnNode;

			origin.addValue(String.valueOf(intInsnNode.operand));
		}
		else if ((opcode == Opcodes.GETFIELD) ||
				 (opcode == Opcodes.GETSTATIC)) {

			origin.addReference(_getFieldKey((FieldInsnNode)abstractInsnNode));
		}
		else if (opcode == Opcodes.CHECKCAST) {
			origin.addAll(
				_getOrigin(
					frames, insnList,
					_getArgument(
						frames[insnList.indexOf(abstractInsnNode)], 0, 1),
					visitedAbstractInsnNodes));
		}
		else if (abstractInsnNode instanceof MethodInsnNode) {
			origin.addAll(
				_getReturnValueOrigin(
					frames, insnList, (MethodInsnNode)abstractInsnNode,
					visitedAbstractInsnNodes));
		}
		else if (_isConcatenation(abstractInsnNode)) {
			InvokeDynamicInsnNode invokeDynamicInsnNode =
				(InvokeDynamicInsnNode)abstractInsnNode;

			Frame<SourceValue> frame =
				frames[insnList.indexOf(invokeDynamicInsnNode)];
			int count = Type.getArgumentCount(invokeDynamicInsnNode.desc);

			List<SourceValue> sourceValues = new ArrayList<>();

			for (int i = 0; i < count; i++) {
				sourceValues.add(_getArgument(frame, i, count));
			}

			origin.addAll(
				_getComputedOrigin(
					frames, insnList, sourceValues, visitedAbstractInsnNodes));
		}
		else if (opcode != Opcodes.ACONST_NULL) {
			origin.setComputed(true);
		}

		return origin;
	}

	private Origin _getOrigin(
		Frame<SourceValue>[] frames, InsnList insnList, SourceValue sourceValue,
		Set<AbstractInsnNode> visitedAbstractInsnNodes) {

		Origin origin = new Origin();

		for (AbstractInsnNode abstractInsnNode : sourceValue.insns) {
			if (!visitedAbstractInsnNodes.add(abstractInsnNode)) {
				continue;
			}

			origin.addAll(
				_getOrigin(
					frames, insnList, abstractInsnNode,
					visitedAbstractInsnNodes));
		}

		return origin;
	}

	private SourceValue _getReceiver(Frame<SourceValue> frame, int count) {
		return frame.getStack(frame.getStackSize() - count - 1);
	}

	private String _getReferenceClassName(String reference) {
		int index = reference.indexOf(CharPool.OPEN_PARENTHESIS);

		if (index == -1) {
			index = reference.length();
		}

		String name = reference.substring(0, index);

		return name.substring(0, name.lastIndexOf(CharPool.PERIOD));
	}

	private String _getReferenceMethodName(String reference) {
		String name = reference.substring(
			0, reference.indexOf(CharPool.OPEN_PARENTHESIS));

		return name.substring(name.lastIndexOf(CharPool.PERIOD) + 1);
	}

	private Origin _getReferenceOrigin(String reference) {
		Origin origin = new Origin();

		boolean found = false;

		Origin trackedOrigin = _referenceOrigins.get(reference);

		if ((trackedOrigin != null) && _isResolvable(reference)) {
			origin.addAll(trackedOrigin);

			found = true;
		}

		int index = reference.indexOf(CharPool.POUND);

		if (index != -1) {
			String methodName = reference.substring(
				0, reference.indexOf(CharPool.OPEN_PARENTHESIS));

			Origin springOrigin = _springOrigins.get(
				methodName + reference.substring(index));

			if (springOrigin != null) {
				origin.addAll(springOrigin);

				found = true;
			}
		}
		else if (reference.indexOf(CharPool.OPEN_PARENTHESIS) != -1) {
			String settingKey = _configurationSettingKeys.get(
				reference.substring(
					0, reference.indexOf(CharPool.OPEN_PARENTHESIS)));

			if (settingKey != null) {
				origin.addPropertyKey(settingKey);

				found = true;
			}
			else if (!found) {
				Origin inheritedOrigin = _getInheritedReturnValueOrigin(
					reference);

				if ((inheritedOrigin == null) &&
					reference.endsWith(_SUFFIX_KEY_ALGORITHM)) {

					inheritedOrigin = _getImplementationReturnValueOrigin(
						reference);
				}

				if (inheritedOrigin != null) {
					origin.addAll(inheritedOrigin);

					found = true;
				}
			}
		}

		if (!found) {
			return null;
		}

		return origin;
	}

	private List<RelatedCryptoMaterialType> _getRelatedCryptoMaterialTypes(
		Frame<SourceValue>[] frames, InsnList insnList, String jcaClassName,
		MethodInsnNode methodInsnNode) {

		if (!Objects.equals(jcaClassName, "KeyFactory")) {
			return Collections.emptyList();
		}

		Set<RelatedCryptoMaterialType> relatedCryptoMaterialTypes =
			new TreeSet<>();

		for (MethodInsnNode linkedMethodInsnNode :
				_getLinkedMethodInsnNodes(frames, insnList, methodInsnNode)) {

			RelatedCryptoMaterialType relatedCryptoMaterialType =
				_keyFactoryRelatedCryptoMaterialTypes.get(
					linkedMethodInsnNode.name);

			if (relatedCryptoMaterialType == null) {
				continue;
			}

			relatedCryptoMaterialTypes.add(relatedCryptoMaterialType);
		}

		return new ArrayList<>(relatedCryptoMaterialTypes);
	}

	private Origin _getReturnValueOrigin(
		Frame<SourceValue>[] frames, InsnList insnList,
		MethodInsnNode methodInsnNode,
		Set<AbstractInsnNode> visitedAbstractInsnNodes) {

		Origin origin = new Origin();

		String propertyKey = _jdkPropertyKeys.get(
			methodInsnNode.owner + StringPool.PERIOD + methodInsnNode.name);

		if (propertyKey != null) {
			origin.addPropertyKey(propertyKey);

			return origin;
		}

		Frame<SourceValue> frame = frames[insnList.indexOf(methodInsnNode)];
		int count = Type.getArgumentCount(methodInsnNode.desc);

		if (methodInsnNode.owner.endsWith("/PropsUtil") &&
			Objects.equals(methodInsnNode.name, "get") && (count > 0)) {

			Origin keyOrigin = _getOrigin(
				frames, insnList, _getArgument(frame, 0, count),
				visitedAbstractInsnNodes);

			for (String value : keyOrigin.getValues()) {
				origin.addPropertyKey(value);
			}

			if (!keyOrigin.isConstant()) {
				origin.addUnresolvedReason(
					"a property whose name the code computes");
			}

			return origin;
		}

		if (methodInsnNode.owner.endsWith("/MapUtil") &&
			methodInsnNode.name.startsWith("get") && (count > 1)) {

			Origin keyOrigin = _getOrigin(
				frames, insnList, _getArgument(frame, 1, count),
				visitedAbstractInsnNodes);

			for (String value : keyOrigin.getValues()) {
				origin.addPropertyKey(
					CryptoSettingUtil.toSettingKey(
						"OSGi configuration", value));
			}

			if (!keyOrigin.isConstant()) {
				origin.addUnresolvedReason(
					"a configuration property whose name the code computes");
			}

			return origin;
		}

		if (methodInsnNode.owner.endsWith("/ParamUtil") &&
			methodInsnNode.name.startsWith("get") && (count > 1)) {

			Origin nameOrigin = _getOrigin(
				frames, insnList, _getArgument(frame, 1, count),
				visitedAbstractInsnNodes);

			for (String value : nameOrigin.getValues()) {
				origin.addUnresolvedReason(
					"the request parameter " + value +
						", which the user chooses");
			}

			if (nameOrigin.getValues(
				).isEmpty()) {

				origin.addUnresolvedReason("a request parameter");
			}

			return origin;
		}

		if (_keyClassNames.contains(methodInsnNode.owner) &&
			Objects.equals(methodInsnNode.name, "getAlgorithm") &&
			(count == 0)) {

			return _getKeyAlgorithmOrigin(
				frames, insnList, _getReceiver(frame, count), new HashSet<>());
		}

		if (_jcaClassNames.containsKey(methodInsnNode.owner) &&
			Objects.equals(methodInsnNode.name, "getAlgorithm") &&
			(count == 0)) {

			for (AbstractInsnNode abstractInsnNode :
					_getReceiver(frame, count).insns) {

				if ((abstractInsnNode instanceof MethodInsnNode) &&
					(_getJCAClassName((MethodInsnNode)abstractInsnNode) !=
						null)) {

					origin.addAll(
						_getAlgorithmOrigin(
							frames, insnList,
							(MethodInsnNode)abstractInsnNode));
				}
				else {
					origin.addUnresolvedReason(
						"the algorithm of a " +
							_jcaClassNames.get(methodInsnNode.owner) +
								" that the code does not create");
				}
			}

			return origin;
		}

		Function<String, String> function = _stringFunctions.get(
			methodInsnNode.owner + StringPool.PERIOD + methodInsnNode.name);

		if (function != null) {
			SourceValue sourceValue = null;

			if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC) {
				sourceValue = _getArgument(frame, 0, count);
			}
			else {
				sourceValue = _getReceiver(frame, count);
			}

			Origin wrappedOrigin = _getOrigin(
				frames, insnList, sourceValue, visitedAbstractInsnNodes);

			wrappedOrigin.transformValues(function);

			return wrappedOrigin;
		}

		if (_isComputation(methodInsnNode)) {
			List<SourceValue> sourceValues = new ArrayList<>();

			if (methodInsnNode.getOpcode() != Opcodes.INVOKESTATIC) {
				sourceValues.add(_getReceiver(frame, count));
			}

			Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);

			for (int i = 0; i < count; i++) {
				if (!Objects.equals(
						argumentTypes[i], Type.getType(String.class))) {

					continue;
				}

				sourceValues.add(_getArgument(frame, i, count));
			}

			return _getComputedOrigin(
				frames, insnList, sourceValues, visitedAbstractInsnNodes);
		}

		origin.addReference(
			_getMethodKey(
				methodInsnNode.owner, methodInsnNode.name,
				methodInsnNode.desc));

		return origin;
	}

	private List<String> _getSubtypeNames(String internalName) {
		List<String> subtypeNames = _subtypeNames.get(internalName);

		if (subtypeNames != null) {
			return subtypeNames;
		}

		subtypeNames = new ArrayList<>();

		for (String curInternalName : _superTypeNames.keySet()) {
			List<String> superTypeNames = _getSuperTypeNames(curInternalName);

			if (!superTypeNames.contains(internalName)) {
				continue;
			}

			subtypeNames.add(curInternalName);
		}

		Collections.sort(subtypeNames);

		_subtypeNames.put(internalName, subtypeNames);

		return subtypeNames;
	}

	private List<String> _getSuperTypeNames(String internalName) {
		List<String> superTypeNames = new ArrayList<>();

		LinkedList<String> internalNames = new LinkedList<>(
			_superTypeNames.getOrDefault(
				internalName, Collections.emptyList()));

		while (!internalNames.isEmpty()) {
			String superTypeName = internalNames.removeFirst();

			if (superTypeNames.contains(superTypeName)) {
				continue;
			}

			superTypeNames.add(superTypeName);

			internalNames.addAll(
				_superTypeNames.getOrDefault(
					superTypeName, Collections.emptyList()));
		}

		return superTypeNames;
	}

	private String _getUnresolvedReason(String reference) {
		int index = reference.indexOf(CharPool.POUND);

		if (index != -1) {
			String methodName = reference.substring(
				0, reference.indexOf(CharPool.OPEN_PARENTHESIS));

			if (methodName.endsWith(".<init>")) {
				methodName =
					"the constructor of " +
						StringUtil.removeLast(methodName, ".<init>");
			}

			String parameter = reference.substring(index + 1);

			if (parameter.endsWith(_SUFFIX_KEY_ALGORITHM)) {
				int parameterIndex = GetterUtil.getInteger(
					StringUtil.removeLast(parameter, _SUFFIX_KEY_ALGORITHM));

				return StringBundler.concat(
					"the algorithm of the key in parameter ",
					parameterIndex + 1, " of ", methodName);
			}

			return StringBundler.concat(
				"parameter ", GetterUtil.getInteger(parameter) + 1, " of ",
				methodName);
		}

		index = reference.indexOf(CharPool.OPEN_PARENTHESIS);

		if (reference.endsWith(_SUFFIX_KEY_ALGORITHM)) {
			if (index != -1) {
				return "the key that " + reference.substring(0, index) +
					"() returns";
			}

			return "the key in the field " +
				StringUtil.removeLast(reference, _SUFFIX_KEY_ALGORITHM);
		}

		if (index != -1) {
			return "the return value of " + reference.substring(0, index) +
				"()";
		}

		return "the field " + reference;
	}

	private boolean _hasCryptoReference(byte[] bytes) {
		String classFileContent = new String(
			bytes, StandardCharsets.ISO_8859_1);

		return _containsAny(classFileContent, _needles);
	}

	private boolean _isClassFileName(String name) {
		if (name.startsWith("com/liferay/") && name.endsWith(".class")) {
			return true;
		}

		return false;
	}

	private boolean _isCompatible(String ownerName, String declaringName) {
		if (Objects.equals(ownerName, declaringName)) {
			return true;
		}

		List<String> superTypeNames = _getSuperTypeNames(declaringName);

		if (superTypeNames.contains(ownerName)) {
			return true;
		}

		superTypeNames = _getSuperTypeNames(ownerName);

		return superTypeNames.contains(declaringName);
	}

	private boolean _isComputation(MethodInsnNode methodInsnNode) {
		if (!methodInsnNode.desc.endsWith(")Ljava/lang/String;")) {
			return false;
		}

		if (Objects.equals(methodInsnNode.owner, "java/lang/String") ||
			methodInsnNode.owner.endsWith("/StringUtil")) {

			return true;
		}

		return false;
	}

	private boolean _isConcatenation(AbstractInsnNode abstractInsnNode) {
		if (!(abstractInsnNode instanceof InvokeDynamicInsnNode)) {
			return false;
		}

		InvokeDynamicInsnNode invokeDynamicInsnNode =
			(InvokeDynamicInsnNode)abstractInsnNode;

		return Objects.equals(
			invokeDynamicInsnNode.name, "makeConcatWithConstants");
	}

	private boolean _isFIPSEnabled(AbstractInsnNode abstractInsnNode) {
		if (abstractInsnNode.getOpcode() != Opcodes.GETSTATIC) {
			return false;
		}

		FieldInsnNode fieldInsnNode = (FieldInsnNode)abstractInsnNode;

		if (fieldInsnNode.owner.endsWith("/PropsValues") &&
			Objects.equals(fieldInsnNode.name, "FIPS_ENABLED")) {

			return true;
		}

		return false;
	}

	private boolean _isFollowedFileName(String name) {
		if (_isClassFileName(name) || name.endsWith(".jsp") ||
			name.endsWith(".jspf")) {

			return true;
		}

		return false;
	}

	private boolean _isKey(Type type) {
		if ((type.getSort() == Type.OBJECT) &&
			_keyClassNames.contains(type.getInternalName())) {

			return true;
		}

		return false;
	}

	private boolean _isResolvable(String reference) {
		int index = reference.indexOf(CharPool.POUND);

		if ((index == -1) || _followedReferences.contains(reference)) {
			return true;
		}

		return _privateMethodKeys.contains(reference.substring(0, index));
	}

	private boolean _isScannedFileName(String name) {
		if (_isClassFileName(name) ||
			(name.endsWith(".xml") && name.contains("spring"))) {

			return true;
		}

		return false;
	}

	private boolean _isTracked(Type type) {
		if (Objects.equals(type, Type.INT_TYPE) ||
			Objects.equals(type, Type.getType(String.class))) {

			return true;
		}

		return false;
	}

	private void _recordConfigurationAttributes(ClassNode classNode) {
		String pid = _getAnnotationValue(
			_getAnnotationNodes(
				classNode.invisibleAnnotations, classNode.visibleAnnotations),
			_DESC_META_OCD, "id");

		if (pid == null) {
			return;
		}

		if (pid.isEmpty()) {
			pid = _getKeyName(classNode.name);
		}

		for (MethodNode methodNode : classNode.methods) {
			String attribute = _getAnnotationValue(
				_getAnnotationNodes(
					methodNode.invisibleAnnotations,
					methodNode.visibleAnnotations),
				_DESC_META_AD, "id");

			if (Validator.isNull(attribute)) {
				attribute = methodNode.name;
			}

			_configurationSettingKeys.put(
				_getKeyName(classNode.name) + StringPool.PERIOD +
					methodNode.name,
				CryptoSettingUtil.toSettingKey(pid + ".config", attribute));
		}
	}

	private Origin _resolve(Origin origin, Set<String> visitedReferences) {
		Origin resolvedOrigin = new Origin();

		resolvedOrigin.addAll(origin);

		resolvedOrigin.clearReferences();

		for (String reference : origin.getReferences()) {
			if (!visitedReferences.add(reference)) {
				continue;
			}

			Origin referenceOrigin = _getReferenceOrigin(reference);

			if (referenceOrigin == null) {
				resolvedOrigin.addUnresolvedReason(
					_getUnresolvedReason(reference));
			}
			else {
				resolvedOrigin.addAll(
					_resolve(referenceOrigin, visitedReferences));
			}
		}

		for (String reference : origin.getDerivedReferences()) {
			if (!visitedReferences.add(reference)) {
				continue;
			}

			Origin referenceOrigin = _getReferenceOrigin(reference);

			if (referenceOrigin == null) {
				resolvedOrigin.addUnresolvedReason(_REASON_COMPUTED);

				continue;
			}

			Origin derivedOrigin = _resolve(referenceOrigin, visitedReferences);

			if (derivedOrigin.getPropertyKeys(
				).isEmpty()) {

				resolvedOrigin.addUnresolvedReason(_REASON_COMPUTED);

				continue;
			}

			for (String propertyKey : derivedOrigin.getPropertyKeys()) {
				resolvedOrigin.addPropertyKey(propertyKey);
			}

			resolvedOrigin.setComputed(true);
		}

		return resolvedOrigin;
	}

	private void _scan(
		ClassNode classNode, String bundleSymbolicName, String featureName) {

		String className = _getClassName(classNode.name);

		Set<String> excludedOpenSAMLURIs = new TreeSet<>();
		boolean openSAML = false;

		for (MethodNode methodNode : classNode.methods) {
			if (methodNode.instructions.size() == 0) {
				continue;
			}

			List<MethodInsnNode> methodInsnNodes = _getCryptoMethodInsnNodes(
				classNode, methodNode);

			Frame<SourceValue>[] frames = null;

			try {
				frames = _analyze(classNode, methodNode);
			}
			catch (AnalyzerException analyzerException) {
				for (MethodInsnNode methodInsnNode : methodInsnNodes) {
					Origin origin = new Origin();

					origin.addUnresolvedReason(
						"bytecode that the scan cannot analyze: " +
							analyzerException.getMessage());

					_scannedCryptoCalls.add(
						new ScannedCryptoCall(
							origin,
							new CryptoCall(
								bundleSymbolicName, className, featureName,
								_getJCAClassName(methodInsnNode)),
							new Origin()));
				}

				continue;
			}

			_scanAssignments(classNode, frames, methodNode);

			InsnList insnList = methodNode.instructions;

			for (AbstractInsnNode abstractInsnNode : insnList) {
				if (frames[insnList.indexOf(abstractInsnNode)] == null) {
					continue;
				}

				if (abstractInsnNode instanceof LdcInsnNode) {
					LdcInsnNode ldcInsnNode = (LdcInsnNode)abstractInsnNode;

					if (_openSAMLAlgorithms.containsKey(
							String.valueOf(ldcInsnNode.cst))) {

						excludedOpenSAMLURIs.add(
							String.valueOf(ldcInsnNode.cst));
					}
				}
				else if (abstractInsnNode instanceof MethodInsnNode) {
					MethodInsnNode methodInsnNode =
						(MethodInsnNode)abstractInsnNode;

					if (Objects.equals(
							methodInsnNode.owner,
							_CLASS_NAME_DEFAULT_SECURITY_CONFIGURATION_BOOTSTRAP) &&
						methodInsnNode.name.startsWith("buildDefault")) {

						openSAML = true;
					}
					else if (Objects.equals(
								methodInsnNode.owner,
								_CLASS_NAME_FIPS_MODE_VALIDATOR) &&
							 Objects.equals(
								 methodInsnNode.name, "validateJWSAlgorithm")) {

						_addVerifiedJWSCryptoCalls(
							bundleSymbolicName, className, featureName);
					}
				}
			}

			for (MethodInsnNode methodInsnNode : methodInsnNodes) {
				if (frames[insnList.indexOf(methodInsnNode)] == null) {
					continue;
				}

				String jcaClassName = _getJCAClassName(methodInsnNode);

				CryptoCall cryptoCall = new CryptoCall(
					bundleSymbolicName, className, featureName, jcaClassName);

				cryptoCall.setCryptoFunctions(
					_getCryptoFunctions(
						frames, insnList, jcaClassName, methodInsnNode));
				cryptoCall.setRelatedCryptoMaterialTypes(
					_getRelatedCryptoMaterialTypes(
						frames, insnList, jcaClassName, methodInsnNode));

				_scannedCryptoCalls.add(
					new ScannedCryptoCall(
						_getAlgorithmOrigin(frames, insnList, methodInsnNode),
						cryptoCall,
						_getKeySizeOrigin(frames, insnList, methodInsnNode)));
			}
		}

		if (openSAML) {
			_addOpenSAMLCryptoCalls(
				bundleSymbolicName, className, excludedOpenSAMLURIs,
				featureName);
		}
	}

	private void _scan(
		String bundleSymbolicName, String featureName, byte[] bytes) {

		ClassReader classReader = new ClassReader(bytes);

		List<String> superTypeNames = new ArrayList<>();

		if (classReader.getSuperName() != null) {
			superTypeNames.add(classReader.getSuperName());
		}

		Collections.addAll(superTypeNames, classReader.getInterfaces());

		_superTypeNames.put(classReader.getClassName(), superTypeNames);

		if (!_hasCryptoReference(bytes)) {
			return;
		}

		ClassNode classNode = new ClassNode();

		classReader.accept(
			classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		_scan(classNode, bundleSymbolicName, featureName);
	}

	private void _scanAssignments(
		ClassNode classNode, Frame<SourceValue>[] frames,
		MethodNode methodNode) {

		String className = _getClassName(classNode.name);

		String methodKey = _getMethodKey(
			classNode.name, methodNode.name, methodNode.desc);

		if ((methodNode.access & Opcodes.ACC_PRIVATE) != 0) {
			_privateMethodKeys.add(methodKey);
		}

		Type returnType = Type.getReturnType(methodNode.desc);

		boolean returnKeyTracked = _isKey(returnType);
		boolean returnValueTracked = _isTracked(returnType);

		InsnList insnList = methodNode.instructions;

		for (AbstractInsnNode abstractInsnNode : insnList) {
			if (frames[insnList.indexOf(abstractInsnNode)] == null) {
				continue;
			}

			int opcode = abstractInsnNode.getOpcode();

			if ((opcode == Opcodes.PUTFIELD) || (opcode == Opcodes.PUTSTATIC)) {
				FieldInsnNode fieldInsnNode = (FieldInsnNode)abstractInsnNode;

				if (!Objects.equals(fieldInsnNode.owner, classNode.name)) {
					continue;
				}

				Type type = Type.getType(fieldInsnNode.desc);

				if (_isTracked(type)) {
					_track(
						_getFieldKey(fieldInsnNode),
						_getArgumentOrigin(
							frames, insnList, abstractInsnNode, 0, 1));
				}
				else if (_isKey(type)) {
					_track(
						_getFieldKey(fieldInsnNode) + _SUFFIX_KEY_ALGORITHM,
						_getKeyAlgorithmOrigin(
							frames, insnList,
							_getArgument(
								frames[insnList.indexOf(abstractInsnNode)], 0,
								1),
							new HashSet<>()));
				}
			}
			else if ((opcode == Opcodes.ARETURN) && returnKeyTracked) {
				_track(
					methodKey + _SUFFIX_KEY_ALGORITHM,
					_getKeyAlgorithmOrigin(
						frames, insnList,
						_getArgument(
							frames[insnList.indexOf(abstractInsnNode)], 0, 1),
						new HashSet<>()));
			}
			else if (((opcode == Opcodes.ARETURN) ||
					  (opcode == Opcodes.IRETURN)) &&
					 returnValueTracked) {

				_track(
					methodKey,
					_getArgumentOrigin(
						frames, insnList, abstractInsnNode, 0, 1));
			}
			else if (abstractInsnNode instanceof MethodInsnNode) {
				MethodInsnNode methodInsnNode =
					(MethodInsnNode)abstractInsnNode;

				if (!Objects.equals(
						_getClassName(methodInsnNode.owner), className)) {

					continue;
				}

				_trackArguments(
					frames, insnList, abstractInsnNode,
					_getMethodKey(
						methodInsnNode.owner, methodInsnNode.name,
						methodInsnNode.desc),
					Type.getArgumentTypes(methodInsnNode.desc), 0);
			}
			else if (abstractInsnNode instanceof InvokeDynamicInsnNode) {
				InvokeDynamicInsnNode invokeDynamicInsnNode =
					(InvokeDynamicInsnNode)abstractInsnNode;

				if ((invokeDynamicInsnNode.bsmArgs.length < 2) ||
					!(invokeDynamicInsnNode.bsmArgs[1] instanceof Handle)) {

					continue;
				}

				Handle handle = (Handle)invokeDynamicInsnNode.bsmArgs[1];

				if (!Objects.equals(handle.getOwner(), classNode.name)) {
					continue;
				}

				int offset = 1;

				if (handle.getTag() == Opcodes.H_INVOKESTATIC) {
					offset = 0;
				}

				_trackArguments(
					frames, insnList, abstractInsnNode,
					_getMethodKey(
						handle.getOwner(), handle.getName(), handle.getDesc()),
					Type.getArgumentTypes(invokeDynamicInsnNode.desc), offset);
			}
		}
	}

	private void _scanSpringXML(byte[] bytes) {
		Document document = null;

		try {
			DocumentBuilderFactory documentBuilderFactory =
				SecureXMLFactoryProviderUtil.newDocumentBuilderFactory();

			DocumentBuilder documentBuilder =
				documentBuilderFactory.newDocumentBuilder();

			document = documentBuilder.parse(new ByteArrayInputStream(bytes));
		}
		catch (IOException | ParserConfigurationException | SAXException
					exception) {

			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}

			return;
		}

		NodeList nodeList = document.getElementsByTagName("bean");

		for (int i = 0; i < nodeList.getLength(); i++) {
			Element element = (Element)nodeList.item(i);

			String className = element.getAttribute("class");

			if (Validator.isNull(className)) {
				continue;
			}

			NodeList childNodeList = element.getChildNodes();

			int index = 0;

			for (int j = 0; j < childNodeList.getLength(); j++) {
				Node childNode = childNodeList.item(j);

				if (childNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}

				Element childElement = (Element)childNode;

				String key = null;

				if (Objects.equals(
						childElement.getTagName(), "constructor-arg")) {

					String indexString = childElement.getAttribute("index");

					if (Validator.isNull(indexString)) {
						indexString = String.valueOf(index);
					}

					index++;

					key = StringBundler.concat(
						className, ".<init>#", indexString);
				}
				else if (Objects.equals(
							childElement.getTagName(), "property")) {

					key = StringBundler.concat(
						className, ".set",
						StringUtil.upperCaseFirstLetter(
							childElement.getAttribute("name")),
						"#0");
				}

				String value = childElement.getAttribute("value");

				if ((key == null) || Validator.isNull(value)) {
					continue;
				}

				Origin origin = _springOrigins.computeIfAbsent(
					key, curKey -> new Origin());

				origin.addValue(value);
			}
		}
	}

	private List<CryptoCall> _toCryptoCalls(
		ScannedCryptoCall scannedCryptoCall) {

		CryptoCall cryptoCall = scannedCryptoCall.getCryptoCall();

		Origin algorithmOrigin = _resolve(
			scannedCryptoCall.getAlgorithmOrigin(), new HashSet<>());
		Origin keySizeOrigin = _resolve(
			scannedCryptoCall.getKeySizeOrigin(), new HashSet<>());

		List<CryptoCall> cryptoCalls = new ArrayList<>();

		SortedSet<String> keySizes = keySizeOrigin.getValues();

		for (String value : algorithmOrigin.getValues()) {
			CryptoCall valueCryptoCall = cryptoCall.copy();

			valueCryptoCall.setAlgorithm(
				CryptoProviderUtil.getAlgorithmName(
					value, cryptoCall.getJCAClassName()));

			if (keySizeOrigin.isConstant() && (keySizes.size() == 1)) {
				valueCryptoCall.setKeySize(
					GetterUtil.getInteger(keySizes.first()));
			}

			cryptoCalls.add(valueCryptoCall);
		}

		Set<String> propertyKeys = algorithmOrigin.getPropertyKeys();

		if (!propertyKeys.isEmpty()) {
			CryptoCall configurableCryptoCall = cryptoCall.copy();

			List<String> configuredBy = new ArrayList<>(propertyKeys);

			configuredBy.sort(
				Comparator.comparing(
					propertyKey -> propertyKey.indexOf(CharPool.AT) != -1));

			for (String propertyKey : keySizeOrigin.getPropertyKeys()) {
				if (configuredBy.contains(propertyKey)) {
					continue;
				}

				configuredBy.add(propertyKey);
			}

			configurableCryptoCall.setConfiguredBy(configuredBy);

			cryptoCalls.add(configurableCryptoCall);
		}

		Set<String> unresolvedReasons = new TreeSet<>(
			algorithmOrigin.getUnresolvedReasons());

		if (unresolvedReasons.isEmpty() && cryptoCalls.isEmpty()) {
			if (algorithmOrigin.isComputed()) {
				unresolvedReasons.add("a value the code computes");
			}
			else {
				unresolvedReasons.add("a value the code never sets");
			}
		}

		if (!unresolvedReasons.isEmpty()) {
			CryptoCall unresolvedCryptoCall = cryptoCall.copy();

			unresolvedCryptoCall.setUnresolvedReason(
				StringUtil.merge(unresolvedReasons, ", "));

			cryptoCalls.add(unresolvedCryptoCall);
		}

		return cryptoCalls;
	}

	private void _track(String reference, Origin origin) {
		Origin referenceOrigin = _referenceOrigins.get(reference);

		if (referenceOrigin == null) {
			_referenceOrigins.put(reference, origin);
		}
		else {
			referenceOrigin.addAll(origin);
		}
	}

	private void _trackArguments(
		Frame<SourceValue>[] frames, InsnList insnList,
		AbstractInsnNode abstractInsnNode, String methodKey,
		Type[] argumentTypes, int offset) {

		for (int i = offset; i < argumentTypes.length; i++) {
			String reference = methodKey + StringPool.POUND + (i - offset);

			if (_isTracked(argumentTypes[i])) {
				_track(
					reference,
					_getArgumentOrigin(
						frames, insnList, abstractInsnNode, i,
						argumentTypes.length));
			}
			else if (_isKey(argumentTypes[i])) {
				_track(
					reference + _SUFFIX_KEY_ALGORITHM,
					_getKeyAlgorithmOrigin(
						frames, insnList,
						_getArgument(
							frames[insnList.indexOf(abstractInsnNode)], i,
							argumentTypes.length),
						new HashSet<>()));
			}
		}
	}

	private void _visit(
			File file, Predicate<String> predicate, EntryVisitor entryVisitor)
		throws IOException {

		String fileName = file.getName();

		if (fileName.endsWith(".lpkg")) {
			try (ZipFile zipFile = new ZipFile(file)) {
				for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
					String name = zipEntry.getName();

					if (!name.endsWith(".jar")) {
						continue;
					}

					try (JarInputStream jarInputStream = new JarInputStream(
							zipFile.getInputStream(zipEntry))) {

						_visit(jarInputStream, name, predicate, entryVisitor);
					}
				}
			}

			return;
		}

		try (JarInputStream jarInputStream = new JarInputStream(
				new FileInputStream(file))) {

			_visit(jarInputStream, fileName, predicate, entryVisitor);
		}
	}

	private void _visit(
			JarInputStream jarInputStream, String name,
			Predicate<String> predicate, EntryVisitor entryVisitor)
		throws IOException {

		Manifest manifest = jarInputStream.getManifest();

		String bundleSymbolicName = _getBundleSymbolicName(name, manifest);
		String featureName = _getFeatureName(name, manifest);

		JarEntry jarEntry = null;

		while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
			if (!predicate.test(jarEntry.getName())) {
				continue;
			}

			entryVisitor.visit(
				bundleSymbolicName, featureName, jarEntry.getName(),
				jarInputStream.readAllBytes());
		}
	}

	private static final String
		_CLASS_NAME_DEFAULT_SECURITY_CONFIGURATION_BOOTSTRAP =
			"org/opensaml/xmlsec/config/DefaultSecurityConfigurationBootstrap";

	private static final String _CLASS_NAME_DIGESTER_UTIL =
		"com/liferay/portal/kernel/util/DigesterUtil";

	private static final String _CLASS_NAME_FIPS_MODE_VALIDATOR =
		"com/liferay/portal/kernel/security/fips/FIPSModeValidator";

	private static final String _CLASS_NAME_SECRET_KEY_SPEC =
		"javax/crypto/spec/SecretKeySpec";

	private static final String _DESC_META_AD =
		"LaQute/bnd/annotation/metatype/Meta$AD;";

	private static final String _DESC_META_OCD =
		"LaQute/bnd/annotation/metatype/Meta$OCD;";

	private static final int _FOLLOW_PASS_COUNT = 10;

	private static final String _REASON_COMPUTED = "a value the code computes";

	private static final String _SUFFIX_KEY_ALGORITHM = "@algorithm";

	private static final Log _log = LogFactoryUtil.getLog(
		CryptoCallScanner.class);

	private static final Map<String, CryptoFunction> _cipherCryptoFunctions =
		HashMapBuilder.put(
			String.valueOf(Cipher.DECRYPT_MODE), CryptoFunction.DECRYPT
		).put(
			String.valueOf(Cipher.ENCRYPT_MODE), CryptoFunction.ENCRYPT
		).put(
			String.valueOf(Cipher.UNWRAP_MODE), CryptoFunction.DECRYPT
		).put(
			String.valueOf(Cipher.WRAP_MODE), CryptoFunction.ENCRYPT
		).build();
	private static final Map<String, CryptoFunction> _cryptoFunctions =
		HashMapBuilder.put(
			"KeyAgreement", CryptoFunction.KEYDERIVE
		).put(
			"KeyFactory", CryptoFunction.OTHER
		).put(
			"KeyGenerator", CryptoFunction.KEYGEN
		).put(
			"KeyManagerFactory", CryptoFunction.OTHER
		).put(
			"KeyPairGenerator", CryptoFunction.KEYGEN
		).put(
			"KeyStore", CryptoFunction.OTHER
		).put(
			"Mac", CryptoFunction.TAG
		).put(
			"MessageDigest", CryptoFunction.DIGEST
		).put(
			"SecretKeyFactory", CryptoFunction.KEYDERIVE
		).put(
			"SecretKeySpec", CryptoFunction.OTHER
		).put(
			"SSLContext", CryptoFunction.OTHER
		).put(
			"TrustManagerFactory", CryptoFunction.OTHER
		).build();
	private static final Map<String, String> _jcaClassNames =
		HashMapBuilder.put(
			"java/security/KeyFactory", "KeyFactory"
		).put(
			"java/security/KeyPairGenerator", "KeyPairGenerator"
		).put(
			"java/security/KeyStore", "KeyStore"
		).put(
			"java/security/MessageDigest", "MessageDigest"
		).put(
			"java/security/Signature", "Signature"
		).put(
			"javax/crypto/Cipher", "Cipher"
		).put(
			"javax/crypto/KeyAgreement", "KeyAgreement"
		).put(
			"javax/crypto/KeyGenerator", "KeyGenerator"
		).put(
			"javax/crypto/Mac", "Mac"
		).put(
			"javax/crypto/SecretKeyFactory", "SecretKeyFactory"
		).put(
			"javax/net/ssl/KeyManagerFactory", "KeyManagerFactory"
		).put(
			"javax/net/ssl/SSLContext", "SSLContext"
		).put(
			"javax/net/ssl/TrustManagerFactory", "TrustManagerFactory"
		).build();
	private static final Map<String, String> _jdkPropertyKeys =
		HashMapBuilder.put(
			"java/security/KeyStore.getDefaultType", "keystore.type"
		).put(
			"javax/net/ssl/KeyManagerFactory.getDefaultAlgorithm",
			"ssl.KeyManagerFactory.algorithm"
		).put(
			"javax/net/ssl/TrustManagerFactory.getDefaultAlgorithm",
			"ssl.TrustManagerFactory.algorithm"
		).build();
	private static final Pattern _jspLiteralPattern = Pattern.compile(
		"^\"([^\"\\\\]*)\"$");
	private static final Map<String, List<LibraryAlgorithm>> _jwsAlgorithms =
		HashMapBuilder.<String, List<LibraryAlgorithm>>put(
			"EdDSA",
			Arrays.asList(
				new LibraryAlgorithm("Ed25519", null, "Signature", 0),
				new LibraryAlgorithm("Ed448", null, "Signature", 0))
		).put(
			"ES256",
			Collections.singletonList(
				new LibraryAlgorithm(
					"SHA256withECDSA", "P-256", "Signature", 0))
		).put(
			"ES384",
			Collections.singletonList(
				new LibraryAlgorithm(
					"SHA384withECDSA", "P-384", "Signature", 0))
		).put(
			"ES512",
			Collections.singletonList(
				new LibraryAlgorithm(
					"SHA512withECDSA", "P-521", "Signature", 0))
		).put(
			"HS256",
			Collections.singletonList(
				new LibraryAlgorithm("HmacSHA256", null, "Mac", 0))
		).put(
			"HS384",
			Collections.singletonList(
				new LibraryAlgorithm("HmacSHA384", null, "Mac", 0))
		).put(
			"HS512",
			Collections.singletonList(
				new LibraryAlgorithm("HmacSHA512", null, "Mac", 0))
		).put(
			"PS256",
			Collections.singletonList(
				new LibraryAlgorithm(
					"SHA256withRSAandMGF1", null, "Signature", 0))
		).put(
			"PS384",
			Collections.singletonList(
				new LibraryAlgorithm(
					"SHA384withRSAandMGF1", null, "Signature", 0))
		).put(
			"PS512",
			Collections.singletonList(
				new LibraryAlgorithm(
					"SHA512withRSAandMGF1", null, "Signature", 0))
		).put(
			"RS256",
			Collections.singletonList(
				new LibraryAlgorithm("SHA256withRSA", null, "Signature", 0))
		).put(
			"RS384",
			Collections.singletonList(
				new LibraryAlgorithm("SHA384withRSA", null, "Signature", 0))
		).put(
			"RS512",
			Collections.singletonList(
				new LibraryAlgorithm("SHA512withRSA", null, "Signature", 0))
		).build();
	private static final Set<String> _keyClassNames = SetUtil.fromArray(
		"java/security/Key", "java/security/PrivateKey",
		"java/security/PublicKey", "javax/crypto/SecretKey",
		_CLASS_NAME_SECRET_KEY_SPEC);
	private static final Map<String, RelatedCryptoMaterialType>
		_keyFactoryRelatedCryptoMaterialTypes = HashMapBuilder.put(
			"generatePrivate", RelatedCryptoMaterialType.PRIVATE_KEY
		).put(
			"generatePublic", RelatedCryptoMaterialType.PUBLIC_KEY
		).build();
	private static final Map<String, String> _keySizeMethodNames =
		HashMapBuilder.put(
			"java/security/KeyPairGenerator", "initialize"
		).put(
			"javax/crypto/KeyGenerator", "init"
		).build();
	private static final Map<String, List<CryptoFunction>>
		_libraryCryptoFunctions =
			HashMapBuilder.<String, List<CryptoFunction>>put(
				"Cipher",
				Arrays.asList(CryptoFunction.DECRYPT, CryptoFunction.ENCRYPT)
			).put(
				"Signature",
				Arrays.asList(CryptoFunction.SIGN, CryptoFunction.VERIFY)
			).build();
	private static final List<String> _needles = Arrays.asList(
		_CLASS_NAME_DEFAULT_SECURITY_CONFIGURATION_BOOTSTRAP,
		_CLASS_NAME_DIGESTER_UTIL, _CLASS_NAME_FIPS_MODE_VALIDATOR,
		"/PropsUtil", "com/nimbusds/jose/JWSAlgorithm", "java/security/",
		"javax/crypto/", "javax/net/ssl/");
	private static final Map<String, LibraryAlgorithm> _openSAMLAlgorithms =
		TreeMapBuilder.<String, LibraryAlgorithm>put(
			"http://www.w3.org/2000/09/xmldsig#dsa-sha1",
			new LibraryAlgorithm("SHA1withDSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2000/09/xmldsig#hmac-sha1",
			new LibraryAlgorithm("HmacSHA1", null, "Mac", 0)
		).put(
			"http://www.w3.org/2000/09/xmldsig#rsa-sha1",
			new LibraryAlgorithm("SHA1withRSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2000/09/xmldsig#sha1",
			new LibraryAlgorithm("SHA-1", null, "MessageDigest", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1",
			new LibraryAlgorithm("SHA1withECDSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256",
			new LibraryAlgorithm("SHA256withECDSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384",
			new LibraryAlgorithm("SHA384withECDSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512",
			new LibraryAlgorithm("SHA512withECDSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#hmac-sha256",
			new LibraryAlgorithm("HmacSHA256", null, "Mac", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#hmac-sha384",
			new LibraryAlgorithm("HmacSHA384", null, "Mac", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#hmac-sha512",
			new LibraryAlgorithm("HmacSHA512", null, "Mac", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
			new LibraryAlgorithm("SHA256withRSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#rsa-sha384",
			new LibraryAlgorithm("SHA384withRSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#rsa-sha512",
			new LibraryAlgorithm("SHA512withRSA", null, "Signature", 0)
		).put(
			"http://www.w3.org/2001/04/xmldsig-more#sha384",
			new LibraryAlgorithm("SHA-384", null, "MessageDigest", 0)
		).put(
			"http://www.w3.org/2001/04/xmlenc#aes128-cbc",
			new LibraryAlgorithm("AES/CBC/ISO10126Padding", null, "Cipher", 128)
		).put(
			"http://www.w3.org/2001/04/xmlenc#aes192-cbc",
			new LibraryAlgorithm("AES/CBC/ISO10126Padding", null, "Cipher", 192)
		).put(
			"http://www.w3.org/2001/04/xmlenc#aes256-cbc",
			new LibraryAlgorithm("AES/CBC/ISO10126Padding", null, "Cipher", 256)
		).put(
			"http://www.w3.org/2001/04/xmlenc#kw-aes128",
			new LibraryAlgorithm("AESWrap", null, "Cipher", 128)
		).put(
			"http://www.w3.org/2001/04/xmlenc#kw-aes192",
			new LibraryAlgorithm("AESWrap", null, "Cipher", 192)
		).put(
			"http://www.w3.org/2001/04/xmlenc#kw-aes256",
			new LibraryAlgorithm("AESWrap", null, "Cipher", 256)
		).put(
			"http://www.w3.org/2001/04/xmlenc#kw-tripledes",
			new LibraryAlgorithm("DESedeWrap", null, "Cipher", 0)
		).put(
			"http://www.w3.org/2001/04/xmlenc#rsa-1_5",
			new LibraryAlgorithm("RSA/ECB/PKCS1Padding", null, "Cipher", 0)
		).put(
			"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p",
			new LibraryAlgorithm(
				"RSA/ECB/OAEPWithSHA-1AndMGF1Padding", null, "Cipher", 0)
		).put(
			"http://www.w3.org/2001/04/xmlenc#sha256",
			new LibraryAlgorithm("SHA-256", null, "MessageDigest", 0)
		).put(
			"http://www.w3.org/2001/04/xmlenc#sha512",
			new LibraryAlgorithm("SHA-512", null, "MessageDigest", 0)
		).put(
			"http://www.w3.org/2001/04/xmlenc#tripledes-cbc",
			new LibraryAlgorithm(
				"DESede/CBC/ISO10126Padding", null, "Cipher", 0)
		).build();
	private static final Map<String, Function<String, String>>
		_stringFunctions = HashMapBuilder.<String, Function<String, String>>put(
			"com/liferay/petra/string/StringUtil.toLowerCase",
			StringUtil::toLowerCase
		).put(
			"com/liferay/petra/string/StringUtil.toUpperCase",
			StringUtil::toUpperCase
		).put(
			"com/liferay/portal/kernel/util/GetterUtil.getInteger",
			Function.identity()
		).put(
			"com/liferay/portal/kernel/util/GetterUtil.getString",
			Function.identity()
		).put(
			"com/liferay/portal/kernel/util/StringUtil.toLowerCase",
			StringUtil::toLowerCase
		).put(
			"com/liferay/portal/kernel/util/StringUtil.toUpperCase",
			StringUtil::toUpperCase
		).put(
			"com/liferay/portal/kernel/util/StringUtil.trim", StringUtil::trim
		).put(
			"java/lang/String.toLowerCase", StringUtil::toLowerCase
		).put(
			"java/lang/String.toUpperCase", StringUtil::toUpperCase
		).put(
			"java/lang/String.trim", StringUtil::trim
		).build();

	private final Map<String, String> _configurationSettingKeys =
		new HashMap<>();
	private final List<File> _files = new ArrayList<>();
	private final boolean _fipsEnabled;
	private final Set<String> _followedReferences = new HashSet<>();
	private final Set<String> _privateMethodKeys = new HashSet<>();
	private final Map<String, Origin> _referenceOrigins = new HashMap<>();
	private final List<byte[]> _scannedClassBytes = new ArrayList<>();
	private final List<ScannedCryptoCall> _scannedCryptoCalls =
		new ArrayList<>();
	private final Map<String, Origin> _springOrigins = new HashMap<>();
	private final Map<String, List<String>> _subtypeNames = new HashMap<>();
	private final Map<String, List<String>> _superTypeNames = new HashMap<>();

	private static class LibraryAlgorithm {

		public LibraryAlgorithm(
			String algorithm, String curve, String jcaClassName, int keySize) {

			_algorithm = algorithm;
			_curve = curve;
			_jcaClassName = jcaClassName;
			_keySize = keySize;
		}

		public String getAlgorithm() {
			return _algorithm;
		}

		public String getCurve() {
			return _curve;
		}

		public String getJCAClassName() {
			return _jcaClassName;
		}

		public int getKeySize() {
			return _keySize;
		}

		private final String _algorithm;
		private final String _curve;
		private final String _jcaClassName;
		private final int _keySize;

	}

	private static class Origin {

		public void addAll(Origin origin) {
			_computed = _computed || origin._computed;
			_derivedReferences.addAll(origin._derivedReferences);
			_propertyKeys.addAll(origin._propertyKeys);
			_references.addAll(origin._references);
			_unresolvedReasons.addAll(origin._unresolvedReasons);
			_values.addAll(origin._values);
		}

		public void addDerivedReference(String reference) {
			_derivedReferences.add(reference);
		}

		public void addPropertyKey(String propertyKey) {
			_propertyKeys.add(propertyKey);
		}

		public void addReference(String reference) {
			_references.add(reference);
		}

		public void addUnresolvedReason(String unresolvedReason) {
			_unresolvedReasons.add(unresolvedReason);
		}

		public void addValue(String value) {
			_values.add(value);
		}

		public void clearReferences() {
			_derivedReferences.clear();
			_references.clear();
		}

		public void clearValues() {
			_values.clear();
		}

		public Set<String> getDerivedReferences() {
			return _derivedReferences;
		}

		public Set<String> getPropertyKeys() {
			return _propertyKeys;
		}

		public Set<String> getReferences() {
			return _references;
		}

		public Set<String> getUnresolvedReasons() {
			return _unresolvedReasons;
		}

		public SortedSet<String> getValues() {
			return _values;
		}

		public boolean isComputed() {
			return _computed;
		}

		public boolean isConstant() {
			if (!_computed && _derivedReferences.isEmpty() &&
				_propertyKeys.isEmpty() && _references.isEmpty() &&
				_unresolvedReasons.isEmpty()) {

				return true;
			}

			return false;
		}

		public void setComputed(boolean computed) {
			_computed = computed;
		}

		public void transformValues(Function<String, String> function) {
			Set<String> values = new TreeSet<>();

			for (String value : _values) {
				values.add(function.apply(value));
			}

			_values.clear();

			_values.addAll(values);
		}

		private boolean _computed;
		private final Set<String> _derivedReferences = new TreeSet<>();
		private final Set<String> _propertyKeys = new TreeSet<>();
		private final Set<String> _references = new TreeSet<>();
		private final Set<String> _unresolvedReasons = new TreeSet<>();
		private final SortedSet<String> _values = new TreeSet<>();

	}

	private static class OriginInterpreter extends SourceInterpreter {

		public OriginInterpreter(String methodKey, MethodNode methodNode) {
			super(Opcodes.ASM9);

			_methodKey = methodKey;

			int local = 0;

			if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
				local++;
			}

			Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);

			for (int i = 0; i < argumentTypes.length; i++) {
				_parameterIndexes.put(local, i);

				local += argumentTypes[i].getSize();
			}
		}

		@Override
		public SourceValue copyOperation(
			AbstractInsnNode abstractInsnNode, SourceValue sourceValue) {

			return sourceValue;
		}

		@Override
		public SourceValue newParameterValue(
			boolean instanceMethod, int local, Type type) {

			Integer parameterIndex = _parameterIndexes.get(local);

			if (parameterIndex == null) {
				return super.newParameterValue(instanceMethod, local, type);
			}

			return new SourceValue(
				type.getSize(),
				new ParameterInsnNode(
					_methodKey + StringPool.POUND + parameterIndex));
		}

		private final String _methodKey;
		private final Map<Integer, Integer> _parameterIndexes = new HashMap<>();

	}

	private static class ParameterInsnNode extends AbstractInsnNode {

		public ParameterInsnNode(String parameterKey) {
			super(-1);

			_parameterKey = parameterKey;
		}

		@Override
		public void accept(MethodVisitor methodVisitor) {
		}

		@Override
		public AbstractInsnNode clone(Map<LabelNode, LabelNode> labelNodes) {
			return this;
		}

		public String getParameterKey() {
			return _parameterKey;
		}

		@Override
		public int getType() {
			return VAR_INSN;
		}

		private final String _parameterKey;

	}

	private static class ScannedCryptoCall {

		public ScannedCryptoCall(
			Origin algorithmOrigin, CryptoCall cryptoCall,
			Origin keySizeOrigin) {

			_algorithmOrigin = algorithmOrigin;
			_cryptoCall = cryptoCall;
			_keySizeOrigin = keySizeOrigin;
		}

		public Origin getAlgorithmOrigin() {
			return _algorithmOrigin;
		}

		public CryptoCall getCryptoCall() {
			return _cryptoCall;
		}

		public Origin getKeySizeOrigin() {
			return _keySizeOrigin;
		}

		private final Origin _algorithmOrigin;
		private final CryptoCall _cryptoCall;
		private final Origin _keySizeOrigin;

	}

	private interface EntryVisitor {

		public void visit(
				String bundleSymbolicName, String featureName, String name,
				byte[] bytes)
			throws IOException;

	}

}