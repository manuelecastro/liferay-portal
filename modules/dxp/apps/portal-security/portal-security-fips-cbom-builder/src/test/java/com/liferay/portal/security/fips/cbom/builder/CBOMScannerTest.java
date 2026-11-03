/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.SecureRandomUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.Arrays;
import java.util.List;

import javax.crypto.KeyGenerator;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CBOMScannerTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testScan() throws Exception {
		_testScan(ConstantTestClass.class, "MessageDigest SHA-256");
		_testScan(
			KeyFactoryTestClass.class, "KeyFactory RSA", "KeyFactory RSA");
		_testScan(KeyGeneratorTestClass.class, "KeyGenerator AES");
		_testScan(
			MultipleValuesTestClass.class, "MessageDigest MD5",
			"MessageDigest SHA-256");
		_testScan(SecureRandomTestClass.class, "SecureRandom SHA1PRNG");
		_testScan(SignatureTestClass.class, "Signature SHA256withRSA");
	}

	public static class ConstantTestClass {

		public void digest() throws Exception {
			String algorithm = "SHA-256";

			MessageDigest.getInstance(algorithm);
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

	public static class MultipleValuesTestClass {

		public void digest(boolean flag) throws Exception {
			MessageDigest.getInstance(flag ? "SHA-256" : "MD5");
		}

	}

	public static class SecureRandomTestClass {

		public void generate() throws Exception {
			SecureRandom.getInstance("SHA1PRNG");

			new SecureRandom();

			SecureRandomUtil.nextLong();
		}

	}

	public static class SignatureTestClass {

		public void verify(PublicKey publicKey) throws Exception {
			Signature signature = Signature.getInstance("SHA256withRSA");

			signature.initVerify(publicKey);
		}

	}

	private String _getClassFileName(Class<?> clazz) {
		String name = StringUtil.replace(
			clazz.getName(), CharPool.PERIOD, CharPool.SLASH);

		return name + ".class";
	}

	private String _getDescription(CryptoUse cryptoUse) {
		return cryptoUse.getJCAClassName() + StringPool.SPACE +
			cryptoUse.getAlgorithm();
	}

	private byte[] _read(Class<?> clazz) throws Exception {
		ClassLoader classLoader = clazz.getClassLoader();

		try (InputStream inputStream = classLoader.getResourceAsStream(
				_getClassFileName(clazz))) {

			return inputStream.readAllBytes();
		}
	}

	private void _testScan(Class<?> clazz, String... expectedDescriptions)
		throws Exception {

		_testScan(new Class<?>[] {clazz}, expectedDescriptions);
	}

	private void _testScan(Class<?>[] classes, String... expectedDescriptions)
		throws Exception {

		CBOMData cbomData = new CBOMData();

		CBOMScanner cbomScanner = new CBOMScanner(cbomData);

		String bundleSymbolicName = RandomTestUtil.randomString();
		String featureName = RandomTestUtil.randomString();

		for (Class<?> curClass : classes) {
			try (InputStream inputStream = new ByteArrayInputStream(
					_read(curClass))) {

				cbomScanner.scan(bundleSymbolicName, featureName, inputStream);
			}
		}

		List<CryptoUse> cryptoUses = cbomData.getScannedCryptoUses();

		for (CryptoUse cryptoUse : cryptoUses) {
			Assert.assertEquals(
				bundleSymbolicName, cryptoUse.getBundleSymbolicName());
			Assert.assertEquals(
				CBOMScannerTest.class.getName(), cryptoUse.getClassName());
			Assert.assertEquals(featureName, cryptoUse.getFeatureName());
		}

		List<String> descriptions = TransformUtil.transform(
			cryptoUses, this::_getDescription);

		descriptions.sort(null);

		Assert.assertEquals(Arrays.asList(expectedDescriptions), descriptions);
	}

}