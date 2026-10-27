/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.fips.FIPSModeValidator;
import com.liferay.portal.kernel.security.xml.SecureXMLFactoryProviderUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.security.xml.SecureXMLFactoryProviderImpl;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import javax.net.ssl.TrustManagerFactory;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.RelatedCryptoMaterialType;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Manuele Castro
 */
public class CryptoCallScannerTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		SecureXMLFactoryProviderUtil secureXMLFactoryProviderUtil =
			new SecureXMLFactoryProviderUtil();

		secureXMLFactoryProviderUtil.setSecureXMLFactoryProvider(
			new SecureXMLFactoryProviderImpl());
	}

	@Test
	public void testGetLimitedSettings() throws Exception {
		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(true);

		cryptoCallScanner.scan(
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			new ByteArrayInputStream(_read(LimitTestClass.class)));

		Assert.assertEquals(
			HashMapBuilder.<String, Set<String>>put(
				"test.algorithm", Collections.singleton("algorithm")
			).put(
				"test.key.algorithm", Collections.singleton("key")
			).build(),
			cryptoCallScanner.getLimitedSettings());
	}

	@Test
	public void testScan() throws Exception {
		_testScan(
			ComputedTestClass.class, false,
			"Mac configured by [test.digest] [tag]");
		_testScan(
			ConfigurationTestClass.class, false,
			"KeyStore configured by [test.keystore.type@com.liferay.test." +
				"TestConfiguration.config] [other]");
		_testScan(
			ConstantTestClass.class, false, "MessageDigest SHA-256 [digest]");
		_testScan(
			ConstructorParameterTestClass.class, false,
			"MessageDigest unresolved: parameter 1 of the constructor of " +
				ConstructorParameterTestClass.class.getName() + " [digest]");
		_testScan(
			DigesterUtilDefaultTestClass.class, false,
			"MessageDigest SHA-1 [digest]");
		_testScan(
			DigesterUtilDefaultTestClass.class, true,
			"MessageDigest SHA-256 [digest]");
		_testScan(
			DigesterUtilTestClass.class, false, "MessageDigest MD5 [digest]");
		_testScan(
			FIPSEnabledTestClass.class, false, "MessageDigest MD5 [digest]");
		_testScan(
			FIPSEnabledTestClass.class, true, "MessageDigest SHA-256 [digest]");
		_testScan(
			InstanceFieldTestClass.class, false,
			"SecretKeySpec configured by [test.algorithm] [other]");
		_testScan(
			InterfaceTestClass.class, false, "MessageDigest SHA-512 [digest]");
		_testScan(
			JWSTestClass.class, false,
			"Signature unresolved: the algorithm in the header of each JSON " +
				"Web Token, which the token's issuer chooses [verify]");
		_testScan(
			JWSTestClass.class, true, "Signature Ed25519 [verify]",
			"Signature Ed448 [verify]",
			"Signature SHA256withECDSA [verify] P-256",
			"Signature SHA256withRSA [verify]",
			"Signature SHA256withRSAandMGF1 [verify]",
			"Signature SHA384withECDSA [verify] P-384",
			"Signature SHA384withRSA [verify]",
			"Signature SHA384withRSAandMGF1 [verify]",
			"Signature SHA512withECDSA [verify] P-521",
			"Signature SHA512withRSA [verify]",
			"Signature SHA512withRSAandMGF1 [verify]");
		_testScan(
			KeyFactoryTestClass.class, false,
			"KeyFactory RSA [other] [private-key]",
			"KeyFactory RSA [other] [public-key]");
		_testScan(
			KeyGeneratorTestClass.class, false,
			"KeyGenerator AES [keygen] 256");
		_testScan(
			KeyTestClass.class, false, "Cipher AES [encrypt]",
			"Cipher Blowfish [encrypt]",
			"Cipher unresolved: the algorithm of the key in parameter 1 of " +
				KeyTestClass.class.getName() + ".encrypt [encrypt]",
			"SecretKeySpec AES [other]", "SecretKeySpec Blowfish [other]");
		_testScan(
			MacTestClass.class, false, "Mac HmacSHA256 [tag]",
			"SecretKeySpec HmacSHA256 [other]");
		_testScan(
			MapUtilTestClass.class, false,
			"MessageDigest configured by [test.algorithm@OSGi configuration] " +
				"[digest]");
		_testScan(
			MultipleValuesTestClass.class, false, "Mac HmacSHA512 [tag]",
			"Mac configured by [test.algorithm] [tag]",
			"MessageDigest SHA-256 [digest]", "MessageDigest SHA-512 [digest]");
		_testScan(
			ParamUtilTestClass.class, false,
			"MessageDigest unresolved: the request parameter algorithm, " +
				"which the user chooses [digest]");
		_testScan(
			ParameterTestClass.class, false,
			"MessageDigest unresolved: parameter 1 of " +
				ParameterTestClass.class.getName() + ".digest [digest]");
		_testScan(
			PrivateMethodTestClass.class, false,
			"Cipher AES/CBC/PKCS5Padding [encrypt]");
		_testScan(
			PropsUtilTestClass.class, false,
			"MessageDigest configured by [test.algorithm] [digest]");
		_testScan(
			SignatureTestClass.class, false,
			"Signature SHA256withRSA [verify]");
		_testScan(
			StaticFieldTestClass.class, false,
			"KeyGenerator configured by [test.algorithm, test.key.size] " +
				"[keygen]");
		_testScan(
			TrustManagerFactoryTestClass.class, false,
			"TrustManagerFactory configured by " +
				"[ssl.TrustManagerFactory.algorithm] [other]");

		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(false);

		cryptoCallScanner.scan(
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			new ByteArrayInputStream(_getOpenSAMLTestClassBytes()));

		List<String> descriptions = TransformUtil.transform(
			cryptoCallScanner.getCryptoCalls(), this::_getDescription);

		Assert.assertEquals(descriptions.toString(), 26, descriptions.size());
		Assert.assertFalse(
			descriptions.contains("Signature SHA1withRSA [sign, verify]"));
		Assert.assertTrue(
			descriptions.contains("Signature SHA256withRSA [sign, verify]"));
	}

	@Test
	public void testScanFile() throws Exception {
		String bundleName = RandomTestUtil.randomString();
		String bundleSymbolicName = RandomTestUtil.randomString();

		File bundleJarFile = temporaryFolder.newFile("bundle.jar");

		_writeJarFile(
			bundleName, bundleSymbolicName + ";singleton:=true",
			ConstantTestClass.class, bundleJarFile);

		_testScanFile(bundleSymbolicName, bundleName, bundleJarFile);

		File libraryJarFile = temporaryFolder.newFile("library.jar");

		_writeJarFile(null, null, ConstantTestClass.class, libraryJarFile);

		_testScanFile("library", "library", libraryJarFile);

		File lpkgFile = temporaryFolder.newFile("package.lpkg");

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(
				new FileOutputStream(lpkgFile))) {

			zipOutputStream.putNextEntry(new ZipEntry("bundle.jar"));

			zipOutputStream.write(Files.readAllBytes(bundleJarFile.toPath()));

			zipOutputStream.closeEntry();
		}

		_testScanFile(bundleSymbolicName, bundleName, lpkgFile);

		_testScanJarFile(
			JSPTestClass.class, "META-INF/resources/test.jsp",
			"<%= jspTestClass.hash(\"SHA-224\") %>",
			"MessageDigest SHA-224 [digest]");
		_testScanJarFile(
			SpringTestClass.class, "META-INF/test-spring.xml",
			StringBundler.concat(
				"<?xml version=\"1.0\"?>\n\n<beans><bean class=\"",
				SpringTestClass.class.getName(),
				"\"><constructor-arg value=\"SHA-512\" /></bean></beans>"),
			"MessageDigest SHA-512 [digest]");
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	public static class ComputedTestClass {

		public void mac() throws Exception {
			Mac.getInstance("Hmac" + PropsUtil.get("test.digest"));
		}

	}

	public static class ConfigurationTestClass {

		public abstract static class BaseKeyStoreManager {

			protected String getKeyStoreType() {
				return testConfiguration.keyStoreType();
			}

			protected TestConfiguration testConfiguration;

		}

		public static class KeyStoreManager extends BaseKeyStoreManager {

			public KeyStore getKeyStore() throws Exception {
				return KeyStore.getInstance(getKeyStoreType());
			}

		}

		@Meta.OCD(id = "com.liferay.test.TestConfiguration")
		public interface TestConfiguration {

			@Meta.AD(id = "test.keystore.type")
			public String keyStoreType();

		}

	}

	public static class ConstantTestClass {

		public void digest() throws Exception {
			String algorithm = "SHA-256";

			MessageDigest.getInstance(algorithm);
		}

	}

	public static class ConstructorParameterTestClass {

		public ConstructorParameterTestClass(String algorithm) {
			_algorithm = algorithm;
		}

		public void digest() throws Exception {
			MessageDigest.getInstance(_algorithm);
		}

		private final String _algorithm;

	}

	public static class DigesterUtilDefaultTestClass {

		public String digest() {
			return DigesterUtil.digest("test");
		}

	}

	public static class DigesterUtilTestClass {

		public String digest() {
			return DigesterUtil.digestHex(DigesterUtil.MD5, "test");
		}

	}

	public static class FIPSEnabledTestClass {

		public void digest() throws Exception {
			MessageDigest.getInstance(
				PropsValues.FIPS_ENABLED ? DigesterUtil.SHA_256 :
					DigesterUtil.MD5);
		}

	}

	public static class InstanceFieldTestClass {

		public Key getKey(byte[] bytes) {
			return new SecretKeySpec(bytes, _getAlgorithm());
		}

		private String _getAlgorithm() {
			String algorithm = _algorithm;

			if (algorithm == null) {
				algorithm = PropsUtil.get("test.algorithm");

				_algorithm = algorithm;
			}

			return algorithm;
		}

		private String _algorithm;

	}

	public static class InterfaceTestClass {

		public void digest(TestDigester testDigester) throws Exception {
			testDigester.digest("SHA-512");
		}

		public static class TestDigesterImpl implements TestDigester {

			@Override
			public void digest(String algorithm) throws Exception {
				MessageDigest.getInstance(algorithm);
			}

		}

		public interface TestDigester {

			public void digest(String algorithm) throws Exception;

		}

	}

	public static class JSPTestClass {

		public void hash(String algorithm) throws Exception {
			MessageDigest.getInstance(algorithm);
		}

	}

	public static class JWSTestClass {

		public void verify(String algorithm) {
			FIPSModeValidator.validateJWSAlgorithm(algorithm);
		}

	}

	public static class KeyFactoryTestClass {

		public PrivateKey getPrivateKey(byte[] bytes) throws Exception {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
		}

		public PublicKey getPublicKey(byte[] bytes) throws Exception {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			return keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
		}

	}

	public static class KeyGeneratorTestClass {

		public void generateKey() throws Exception {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

			keyGenerator.init(256);
		}

	}

	public static class KeyTestClass {

		public void encrypt() throws Exception {
			_encrypt(_getKey());
		}

		public void encrypt(byte[] bytes) throws Exception {
			_encrypt(new SecretKeySpec(bytes, "AES"));
		}

		public void encrypt(Key key) throws Exception {
			_encrypt(key);
		}

		private void _encrypt(Key key) throws Exception {
			Cipher cipher = Cipher.getInstance(key.getAlgorithm());

			cipher.init(Cipher.ENCRYPT_MODE, key);
		}

		private Key _getKey() {
			return new SecretKeySpec(new byte[16], "Blowfish");
		}

	}

	public static class LimitTestClass {

		public void validate() {
			FIPSModeValidator.validateAlgorithm(
				PropsUtil.get("test.algorithm"));
			FIPSModeValidator.validateKey(
				PropsUtil.get("test.key.algorithm"), 128);
		}

	}

	public static class MacTestClass {

		public Key getKey(byte[] bytes) throws Exception {
			Mac mac = Mac.getInstance("HmacSHA256");

			return new SecretKeySpec(bytes, mac.getAlgorithm());
		}

	}

	public static class MapUtilTestClass {

		public void digest(Map<String, Object> properties) throws Exception {
			MessageDigest.getInstance(
				MapUtil.getString(properties, "test.algorithm", "SHA-256"));
		}

	}

	public static class MultipleValuesTestClass {

		public void digest(boolean strong) throws Exception {
			MessageDigest.getInstance(strong ? "SHA-512" : "SHA-256");
		}

		public void mac(boolean strong) throws Exception {
			Mac.getInstance(
				strong ? "HmacSHA512" : PropsUtil.get("test.algorithm"));
		}

	}

	public static class ParamUtilTestClass {

		public void digest(ServiceContext serviceContext) throws Exception {
			MessageDigest.getInstance(
				ParamUtil.getString(serviceContext, "algorithm"));
		}

	}

	public static class ParameterTestClass {

		public void digest(String algorithm) throws Exception {
			MessageDigest.getInstance(algorithm);
		}

	}

	public static class PrivateMethodTestClass {

		public void encrypt(Key key) throws Exception {
			_encrypt(key, "AES/CBC/PKCS5Padding");
		}

		private void _encrypt(Key key, String transformation) throws Exception {
			Cipher cipher = Cipher.getInstance(transformation);

			cipher.init(Cipher.ENCRYPT_MODE, key);
		}

	}

	public static class PropsUtilTestClass {

		public void digest() throws Exception {
			MessageDigest.getInstance(PropsUtil.get("test.algorithm"));
		}

	}

	public static class SignatureTestClass {

		public void verify(PublicKey publicKey) throws Exception {
			Signature signature = Signature.getInstance("SHA256withRSA");

			signature.initVerify(publicKey);
		}

	}

	public static class SpringTestClass {

		public SpringTestClass(String algorithm) {
			_algorithm = algorithm;
		}

		public void digest() throws Exception {
			MessageDigest.getInstance(_algorithm);
		}

		private final String _algorithm;

	}

	public static class StaticFieldTestClass {

		public void generateKey() throws Exception {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(_ALGORITHM);

			keyGenerator.init(_KEY_SIZE);
		}

		private static final String _ALGORITHM = StringUtil.toUpperCase(
			GetterUtil.getString(PropsUtil.get("test.algorithm")));

		private static final int _KEY_SIZE = GetterUtil.getInteger(
			PropsUtil.get("test.key.size"));

	}

	public static class TrustManagerFactoryTestClass {

		public void getTrustManagerFactory() throws Exception {
			TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		}

	}

	private String _getClassFileName(Class<?> clazz) {
		String name = StringUtil.replace(
			clazz.getName(), CharPool.PERIOD, CharPool.SLASH);

		return name + ".class";
	}

	private String _getDescription(CryptoCall cryptoCall) {
		StringBundler sb = new StringBundler(12);

		sb.append(cryptoCall.getJCAClassName());
		sb.append(StringPool.SPACE);

		if (cryptoCall.getAlgorithm() != null) {
			sb.append(cryptoCall.getAlgorithm());
		}
		else if (ListUtil.isNotEmpty(cryptoCall.getConfiguredBy())) {
			sb.append("configured by ");
			sb.append(String.valueOf(cryptoCall.getConfiguredBy()));
		}
		else {
			sb.append("unresolved: ");
			sb.append(cryptoCall.getUnresolvedReason());
		}

		sb.append(StringPool.SPACE);
		sb.append(
			String.valueOf(
				TransformUtil.transform(
					cryptoCall.getCryptoFunctions(), CryptoFunction::getName)));

		if (cryptoCall.getKeySize() > 0) {
			sb.append(StringPool.SPACE);
			sb.append(String.valueOf(cryptoCall.getKeySize()));
		}

		if (cryptoCall.getCurve() != null) {
			sb.append(StringPool.SPACE);
			sb.append(cryptoCall.getCurve());
		}

		if (ListUtil.isNotEmpty(cryptoCall.getRelatedCryptoMaterialTypes())) {
			sb.append(StringPool.SPACE);
			sb.append(
				String.valueOf(
					TransformUtil.transform(
						cryptoCall.getRelatedCryptoMaterialTypes(),
						RelatedCryptoMaterialType::getName)));
		}

		return sb.toString();
	}

	private byte[] _getOpenSAMLTestClassBytes() {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		classWriter.visit(
			Opcodes.V21, Opcodes.ACC_PUBLIC,
			"com/liferay/test/OpenSAMLTestClass", null, "java/lang/Object",
			null);

		MethodVisitor methodVisitor = classWriter.visitMethod(
			Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "configure",
			"()Ljava/lang/Object;", null, null);

		methodVisitor.visitCode();
		methodVisitor.visitLdcInsn(
			"http://www.w3.org/2000/09/xmldsig#rsa-sha1");
		methodVisitor.visitInsn(Opcodes.POP);
		methodVisitor.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			"org/opensaml/xmlsec/config/DefaultSecurityConfigurationBootstrap",
			"buildDefaultSignatureSigningConfiguration", "()Ljava/lang/Object;",
			false);
		methodVisitor.visitInsn(Opcodes.ARETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();

		classWriter.visitEnd();

		return classWriter.toByteArray();
	}

	private byte[] _read(Class<?> clazz) throws Exception {
		ClassLoader classLoader = clazz.getClassLoader();

		try (InputStream inputStream = classLoader.getResourceAsStream(
				_getClassFileName(clazz))) {

			return inputStream.readAllBytes();
		}
	}

	private void _testScan(
			Class<?> clazz, boolean fipsEnabled, String... expectedDescriptions)
		throws Exception {

		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(
			fipsEnabled);

		String bundleSymbolicName = RandomTestUtil.randomString();
		String featureName = RandomTestUtil.randomString();

		List<Class<?>> classes = new ArrayList<>();

		classes.add(DigesterUtil.class);
		classes.add(clazz);

		Collections.addAll(classes, clazz.getDeclaredClasses());

		for (Class<?> curClass : classes) {
			try (InputStream inputStream = new ByteArrayInputStream(
					_read(curClass))) {

				cryptoCallScanner.scan(
					bundleSymbolicName, featureName, inputStream);
			}
		}

		List<CryptoCall> cryptoCalls = cryptoCallScanner.getCryptoCalls();

		for (CryptoCall cryptoCall : cryptoCalls) {
			Assert.assertEquals(
				bundleSymbolicName, cryptoCall.getBundleSymbolicName());
			Assert.assertEquals(
				CryptoCallScannerTest.class.getName(),
				cryptoCall.getClassName());
			Assert.assertEquals(featureName, cryptoCall.getFeatureName());
		}

		List<String> descriptions = TransformUtil.transform(
			cryptoCalls, this::_getDescription);

		descriptions.sort(null);

		Assert.assertEquals(Arrays.asList(expectedDescriptions), descriptions);
	}

	private void _testScanFile(
			String expectedBundleSymbolicName, String expectedFeatureName,
			File file)
		throws Exception {

		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(false);

		cryptoCallScanner.scan(file);

		List<CryptoCall> cryptoCalls = cryptoCallScanner.getCryptoCalls();

		Assert.assertEquals(cryptoCalls.toString(), 1, cryptoCalls.size());

		CryptoCall cryptoCall = cryptoCalls.get(0);

		Assert.assertEquals(
			expectedBundleSymbolicName, cryptoCall.getBundleSymbolicName());
		Assert.assertEquals(
			CryptoCallScannerTest.class.getName(), cryptoCall.getClassName());
		Assert.assertEquals(expectedFeatureName, cryptoCall.getFeatureName());
	}

	private void _testScanJarFile(
			Class<?> clazz, String name, String content,
			String... expectedDescriptions)
		throws Exception {

		File file = temporaryFolder.newFile();

		_writeJarFile(null, null, clazz, file, name, content);

		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(false);

		cryptoCallScanner.scan(file);

		List<String> descriptions = TransformUtil.transform(
			cryptoCallScanner.getCryptoCalls(), this::_getDescription);

		descriptions.sort(null);

		Assert.assertEquals(Arrays.asList(expectedDescriptions), descriptions);
	}

	private void _writeJarFile(
			String bundleName, String bundleSymbolicName, Class<?> clazz,
			File file, String... resources)
		throws Exception {

		Manifest manifest = new Manifest();

		Attributes attributes = manifest.getMainAttributes();

		attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

		if (bundleName != null) {
			attributes.putValue("Bundle-Name", bundleName);
		}

		if (bundleSymbolicName != null) {
			attributes.putValue("Bundle-SymbolicName", bundleSymbolicName);
		}

		try (JarOutputStream jarOutputStream = new JarOutputStream(
				new FileOutputStream(file), manifest)) {

			jarOutputStream.putNextEntry(
				new JarEntry(_getClassFileName(clazz)));

			jarOutputStream.write(_read(clazz));

			jarOutputStream.closeEntry();

			for (int i = 0; i < resources.length; i += 2) {
				jarOutputStream.putNextEntry(new JarEntry(resources[i]));

				jarOutputStream.write(
					resources[i + 1].getBytes(StandardCharsets.UTF_8));

				jarOutputStream.closeEntry();
			}
		}
	}

}