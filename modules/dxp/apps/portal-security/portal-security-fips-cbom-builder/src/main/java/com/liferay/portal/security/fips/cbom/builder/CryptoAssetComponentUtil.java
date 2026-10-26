/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.crypto.AlgorithmProperties;
import org.cyclonedx.model.component.crypto.CryptoProperties;
import org.cyclonedx.model.component.crypto.ProtocolProperties;
import org.cyclonedx.model.component.crypto.RelatedCryptoMaterialProperties;
import org.cyclonedx.model.component.crypto.enums.AssetType;
import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Mode;
import org.cyclonedx.model.component.crypto.enums.Padding;
import org.cyclonedx.model.component.crypto.enums.Primitive;
import org.cyclonedx.model.component.crypto.enums.ProtocolType;
import org.cyclonedx.model.component.crypto.enums.RelatedCryptoMaterialType;
import org.cyclonedx.model.component.evidence.Occurrence;

/**
 * Turns the crypto usages into CycloneDX 1.6 <code>cryptographic-asset</code>
 * components, typed the way CycloneDX's guide to CBOM types them: an
 * algorithm, related crypto material for keys and key stores, or a protocol
 * for an SSL context. Uses of the same JCA class and algorithm share one
 * component, which lists every class that uses it and takes the name
 * CycloneDX's cryptography definitions give the algorithm. Uses whose
 * algorithm a setting chooses share one component per JCA class and setting,
 * which is named after the setting. A key points at the algorithm its class
 * uses it with, when there is exactly one.
 *
 * @author Manuele Castro
 */
public class CryptoAssetComponentUtil {

	public static String getBOMRef(CryptoUsage cryptoUsage) throws Exception {
		StringBundler sb = new StringBundler(7);

		sb.append("crypto:");
		sb.append(cryptoUsage.getJCAClassName());
		sb.append(StringPool.COLON);

		if (cryptoUsage.isConfigurable()) {
			List<String> configuredBy = cryptoUsage.getConfiguredBy();

			sb.append(configuredBy.get(0));

			return sb.toString();
		}

		sb.append(
			CryptoProviderUtil.getAlgorithmName(
				cryptoUsage.getAlgorithm(), cryptoUsage.getJCAClassName()));

		if (cryptoUsage.getKeySize() > 0) {
			sb.append(StringPool.COLON);
			sb.append(cryptoUsage.getKeySize());
		}

		if (cryptoUsage.getCurve() != null) {
			sb.append(StringPool.COLON);
			sb.append(cryptoUsage.getCurve());
		}

		return sb.toString();
	}

	public static List<Component> toComponents(List<CryptoUsage> cryptoUsages)
		throws Exception {

		Map<String, List<CryptoUsage>> cryptoUsagesMap = new TreeMap<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			List<CryptoUsage> bomRefCryptoUsages =
				cryptoUsagesMap.computeIfAbsent(
					getBOMRef(cryptoUsage), bomRef -> new ArrayList<>());

			bomRefCryptoUsages.add(cryptoUsage);
		}

		return TransformUtil.unsafeTransform(
			cryptoUsagesMap.entrySet(),
			entry -> _toComponent(
				entry.getKey(), entry.getValue(), cryptoUsages));
	}

	private static void _addProperty(
		String name, Map<String, Set<String>> propertiesMap, String value) {

		Set<String> values = propertiesMap.computeIfAbsent(
			"liferay:cbom:" + name, key -> new TreeSet<>());

		values.add(value);
	}

	private static AlgorithmProperties _getAlgorithmProperties(
		List<CryptoUsage> cryptoUsages) {

		AlgorithmProperties algorithmProperties = new AlgorithmProperties();

		algorithmProperties.setCryptoFunctions(
			_getCryptoFunctions(cryptoUsages));
		algorithmProperties.setPrimitive(_getPrimitive(cryptoUsages));

		CryptoUsage cryptoUsage = cryptoUsages.get(0);

		if (cryptoUsage.isConfigurable()) {
			return algorithmProperties;
		}

		algorithmProperties.setCurve(cryptoUsage.getCurve());

		if (cryptoUsage.getKeySize() > 0) {
			algorithmProperties.setParameterSetIdentifier(
				String.valueOf(cryptoUsage.getKeySize()));
		}

		if (Objects.equals(cryptoUsage.getJCAClassName(), "Cipher")) {
			_setCipherProperties(
				algorithmProperties, cryptoUsage.getAlgorithm());
		}

		return algorithmProperties;
	}

	private static String _getAlgorithmRef(
			List<CryptoUsage> cryptoUsages, List<CryptoUsage> keyCryptoUsages)
		throws Exception {

		TreeSet<String> algorithmRefs = new TreeSet<>();

		for (CryptoUsage keyCryptoUsage : keyCryptoUsages) {
			for (String className : keyCryptoUsage.getClassNames()) {
				List<CryptoUsage> candidateCryptoUsages = new ArrayList<>();
				List<CryptoUsage> matchingCryptoUsages = new ArrayList<>();

				for (CryptoUsage cryptoUsage : cryptoUsages) {
					if (!_isKeyCandidate(
							className, cryptoUsage, keyCryptoUsage)) {

						continue;
					}

					candidateCryptoUsages.add(cryptoUsage);

					if (!_isKeyAlgorithm(cryptoUsage, keyCryptoUsage)) {
						continue;
					}

					matchingCryptoUsages.add(cryptoUsage);
				}

				if (matchingCryptoUsages.isEmpty() &&
					(candidateCryptoUsages.size() == 1)) {

					matchingCryptoUsages = candidateCryptoUsages;
				}

				for (CryptoUsage matchingCryptoUsage : matchingCryptoUsages) {
					algorithmRefs.add(getBOMRef(matchingCryptoUsage));
				}
			}
		}

		if (algorithmRefs.size() != 1) {
			return null;
		}

		return algorithmRefs.first();
	}

	private static List<CryptoFunction> _getCryptoFunctions(
		List<CryptoUsage> cryptoUsages) {

		Set<CryptoFunction> cryptoFunctions = new TreeSet<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			cryptoFunctions.addAll(cryptoUsage.getCryptoFunctions());
		}

		return new ArrayList<>(cryptoFunctions);
	}

	private static Evidence _getEvidence(List<CryptoUsage> cryptoUsages) {
		Evidence evidence = new Evidence();

		Set<String> classNames = new TreeSet<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			classNames.addAll(cryptoUsage.getClassNames());
		}

		evidence.setOccurrences(
			TransformUtil.transform(
				classNames,
				className -> {
					Occurrence occurrence = new Occurrence();

					occurrence.setLocation(className);

					return occurrence;
				}));

		return evidence;
	}

	private static Mode _getMode(String name) {
		for (Mode mode : Mode.values()) {
			if (StringUtil.equalsIgnoreCase(mode.getName(), name)) {
				return mode;
			}
		}

		return Mode.OTHER;
	}

	private static String _getName(CryptoUsage cryptoUsage) throws Exception {
		if (cryptoUsage.isConfigurable()) {
			List<String> configuredBy = cryptoUsage.getConfiguredBy();

			return configuredBy.get(0);
		}

		return CryptoAlgorithmNameUtil.getName(
			CryptoProviderUtil.getAlgorithmName(
				cryptoUsage.getAlgorithm(), cryptoUsage.getJCAClassName()),
			cryptoUsage.getJCAClassName(), cryptoUsage.getKeySize());
	}

	private static Primitive _getPrimitive(List<CryptoUsage> cryptoUsages) {
		CryptoUsage firstCryptoUsage = cryptoUsages.get(0);

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			if (cryptoUsage.getPrimitive() != firstCryptoUsage.getPrimitive()) {
				return Primitive.UNKNOWN;
			}
		}

		return firstCryptoUsage.getPrimitive();
	}

	private static List<Property> _getProperties(
		List<CryptoUsage> cryptoUsages) {

		List<Property> properties = new ArrayList<>();

		Map<String, Set<String>> propertiesMap = new TreeMap<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			_addProperty(
				"jca-class", propertiesMap, cryptoUsage.getJCAClassName());
		}

		for (Map.Entry<String, Set<String>> entry : propertiesMap.entrySet()) {
			for (String value : entry.getValue()) {
				properties.add(new Property(entry.getKey(), value));
			}
		}

		return properties;
	}

	private static ProtocolProperties _getProtocolProperties(
			CryptoUsage cryptoUsage)
		throws Exception {

		ProtocolProperties protocolProperties = new ProtocolProperties();

		if (cryptoUsage.isConfigurable()) {
			protocolProperties.setType(ProtocolType.TLS);

			return protocolProperties;
		}

		String algorithm = CryptoProviderUtil.getAlgorithmName(
			cryptoUsage.getAlgorithm(), cryptoUsage.getJCAClassName());

		if (algorithm.equals("TLS")) {
			protocolProperties.setType(ProtocolType.TLS);
		}
		else if (algorithm.startsWith("TLSv")) {
			protocolProperties.setType(ProtocolType.TLS);
			protocolProperties.setVersion(algorithm.substring(4));
		}
		else {
			protocolProperties.setType(ProtocolType.OTHER);
		}

		return protocolProperties;
	}

	private static RelatedCryptoMaterialProperties
			_getRelatedCryptoMaterialProperties(
				List<CryptoUsage> cryptoUsages,
				List<CryptoUsage> keyCryptoUsages)
		throws Exception {

		RelatedCryptoMaterialProperties relatedCryptoMaterialProperties =
			new RelatedCryptoMaterialProperties();

		CryptoUsage keyCryptoUsage = keyCryptoUsages.get(0);

		if (Objects.equals(keyCryptoUsage.getJCAClassName(), "KeyStore")) {
			if (!keyCryptoUsage.isConfigurable()) {
				relatedCryptoMaterialProperties.setFormat(
					CryptoProviderUtil.getAlgorithmName(
						keyCryptoUsage.getAlgorithm(), "KeyStore"));
			}

			relatedCryptoMaterialProperties.setType(
				RelatedCryptoMaterialType.OTHER);

			return relatedCryptoMaterialProperties;
		}

		relatedCryptoMaterialProperties.setAlgorithmRef(
			_getAlgorithmRef(cryptoUsages, keyCryptoUsages));

		if (Objects.equals(keyCryptoUsage.getJCAClassName(), "SecretKeySpec")) {
			relatedCryptoMaterialProperties.setType(
				RelatedCryptoMaterialType.SECRET_KEY);

			return relatedCryptoMaterialProperties;
		}

		TreeSet<RelatedCryptoMaterialType> relatedCryptoMaterialTypes =
			new TreeSet<>();

		for (CryptoUsage cryptoUsage : keyCryptoUsages) {
			relatedCryptoMaterialTypes.addAll(
				cryptoUsage.getRelatedCryptoMaterialTypes());
		}

		if (relatedCryptoMaterialTypes.size() == 1) {
			relatedCryptoMaterialProperties.setType(
				relatedCryptoMaterialTypes.first());
		}
		else {
			relatedCryptoMaterialProperties.setType(
				RelatedCryptoMaterialType.KEY);
		}

		return relatedCryptoMaterialProperties;
	}

	private static boolean _isKeyAlgorithm(
			CryptoUsage cryptoUsage, CryptoUsage keyCryptoUsage)
		throws Exception {

		if (keyCryptoUsage.isConfigurable()) {
			return true;
		}

		String algorithm = StringUtil.toUpperCase(
			CryptoProviderUtil.getAlgorithmName(
				cryptoUsage.getAlgorithm(), cryptoUsage.getJCAClassName()));
		String keyAlgorithm = StringUtil.toUpperCase(
			CryptoProviderUtil.getAlgorithmName(
				keyCryptoUsage.getAlgorithm(),
				keyCryptoUsage.getJCAClassName()));

		String jcaClassName = cryptoUsage.getJCAClassName();

		if (jcaClassName.equals("Cipher")) {
			String[] transformationParts = StringUtil.split(
				algorithm, CharPool.SLASH);

			return transformationParts[0].equals(keyAlgorithm);
		}

		if (jcaClassName.equals("KeyAgreement")) {
			if (algorithm.equals(keyAlgorithm) ||
				(algorithm.equals("ECDH") && keyAlgorithm.equals("EC"))) {

				return true;
			}

			return false;
		}

		if (jcaClassName.equals("Signature")) {
			if (algorithm.endsWith("WITH" + keyAlgorithm) ||
				(algorithm.endsWith("WITHECDSA") &&
				 keyAlgorithm.equals("EC"))) {

				return true;
			}

			return false;
		}

		return algorithm.equals(keyAlgorithm);
	}

	private static boolean _isKeyCandidate(
		String className, CryptoUsage cryptoUsage, CryptoUsage keyCryptoUsage) {

		List<String> classNames = cryptoUsage.getClassNames();

		if (!_keyAlgorithmJCAClassNames.contains(
				cryptoUsage.getJCAClassName()) ||
			!classNames.contains(className) ||
			(cryptoUsage.isConfigurable() != keyCryptoUsage.isConfigurable())) {

			return false;
		}

		if (!keyCryptoUsage.isConfigurable()) {
			return true;
		}

		List<String> configuredBy = cryptoUsage.getConfiguredBy();

		for (String propertyKey : keyCryptoUsage.getConfiguredBy()) {
			if (configuredBy.contains(propertyKey)) {
				return true;
			}
		}

		return false;
	}

	private static void _setCipherProperties(
		AlgorithmProperties algorithmProperties, String transformation) {

		String[] transformationParts = StringUtil.split(
			StringUtil.toUpperCase(transformation), CharPool.SLASH);

		if (transformationParts.length != 3) {
			return;
		}

		algorithmProperties.setMode(_getMode(transformationParts[1]));

		String paddingName = transformationParts[2];

		if (paddingName.equals("NOPADDING")) {
			return;
		}

		if (paddingName.startsWith("OAEP")) {
			algorithmProperties.setPadding(Padding.OAEP);
		}
		else if (paddingName.equals("PKCS1PADDING")) {
			algorithmProperties.setPadding(Padding.PKCS1V15);
		}
		else if (paddingName.equals("PKCS5PADDING")) {
			algorithmProperties.setPadding(Padding.PKCS5);
		}
		else if (paddingName.equals("PKCS7PADDING")) {
			algorithmProperties.setPadding(Padding.PKCS7);
		}
		else {
			algorithmProperties.setPadding(Padding.OTHER);
		}
	}

	private static Component _toComponent(
			String bomRef, List<CryptoUsage> bomRefCryptoUsages,
			List<CryptoUsage> cryptoUsages)
		throws Exception {

		Component component = new Component();
		CryptoProperties cryptoProperties = new CryptoProperties();

		CryptoUsage cryptoUsage = bomRefCryptoUsages.get(0);

		AssetType assetType = _assetTypes.getOrDefault(
			cryptoUsage.getJCAClassName(), AssetType.ALGORITHM);

		if (assetType == AssetType.ALGORITHM) {
			cryptoProperties.setAlgorithmProperties(
				_getAlgorithmProperties(bomRefCryptoUsages));
		}
		else if (assetType == AssetType.PROTOCOL) {
			cryptoProperties.setProtocolProperties(
				_getProtocolProperties(cryptoUsage));
		}
		else {
			cryptoProperties.setRelatedCryptoMaterialProperties(
				_getRelatedCryptoMaterialProperties(
					cryptoUsages, bomRefCryptoUsages));
		}

		cryptoProperties.setAssetType(assetType);

		component.setBomRef(bomRef);
		component.setCryptoProperties(cryptoProperties);
		component.setEvidence(_getEvidence(bomRefCryptoUsages));
		component.setName(_getName(cryptoUsage));
		component.setProperties(_getProperties(bomRefCryptoUsages));
		component.setType(Component.Type.CRYPTOGRAPHIC_ASSET);

		return component;
	}

	private static final Map<String, AssetType> _assetTypes =
		HashMapBuilder.put(
			"KeyFactory", AssetType.RELATED_CRYPTO_MATERIAL
		).put(
			"KeyStore", AssetType.RELATED_CRYPTO_MATERIAL
		).put(
			"SecretKeySpec", AssetType.RELATED_CRYPTO_MATERIAL
		).put(
			"SSLContext", AssetType.PROTOCOL
		).build();
	private static final Set<String> _keyAlgorithmJCAClassNames =
		SetUtil.fromArray("Cipher", "KeyAgreement", "Mac", "Signature");

}