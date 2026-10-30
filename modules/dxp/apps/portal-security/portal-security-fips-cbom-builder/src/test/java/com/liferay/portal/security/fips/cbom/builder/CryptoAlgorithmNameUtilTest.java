/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoAlgorithmNameUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetName() {
		_testGetName("AES", null, "AES", "Cipher", 0);
		_testGetName("AES", null, "AES-256", "KeyGenerator", 256);
		_testGetName("AES/CBC/PKCS5Padding", null, "AES-CBC", "Cipher", 0);
		_testGetName("AES/GCM/NoPadding", null, "AES-128-GCM", "Cipher", 128);
		_testGetName("AES/GCM/NoPadding", null, "AES-GCM", "Cipher", 0);
		_testGetName("AESWrap", null, "AES-256-KW", "Cipher", 256);
		_testGetName(
			"ChaCha20-Poly1305/None/NoPadding", null, "ChaCha20-Poly1305",
			"Cipher", 0);
		_testGetName("DESede/CBC/PKCS5Padding", null, "3DES-CBC", "Cipher", 0);
		_testGetName("DESedeWrap", null, "3DES-KW", "Cipher", 0);
		_testGetName("DH", null, "FFDH", "KeyAgreement", 0);
		_testGetName("ECDH", "P-384", "ECDH-P-384", "KeyAgreement", 0);
		_testGetName("Ed25519", null, "Ed25519", "Signature", 0);
		_testGetName("HmacSHA1", null, "HMAC-SHA-1", "Mac", 0);
		_testGetName("HmacSHA256", null, "HMAC-SHA-256", "KeyGenerator", 0);
		_testGetName("HmacSHA512", null, "HMAC-SHA-512", "Mac", 0);
		_testGetName("HmacSHA512", null, "HMAC-SHA-512", "SecretKeySpec", 0);
		_testGetName("MD5", null, "MD5", "MessageDigest", 0);
		_testGetName(
			"PBKDF2WithHmacSHA256", null, "PBKDF2-SHA-256", "SecretKeyFactory",
			0);
		_testGetName("PKCS12", null, "PKCS12", "KeyStore", 0);
		_testGetName("RAW", null, "RAW", "SecretKeySpec", 0);
		_testGetName("RSA", null, "RSA-2048", "KeyPairGenerator", 2048);
		_testGetName(
			"RSA/ECB/OAEPWithSHA-256AndMGF1Padding", null, "RSA-OAEP-SHA-256",
			"Cipher", 0);
		_testGetName(
			"RSA/ECB/PKCS1Padding", null, "RSA-PKCS1-1.5", "Cipher", 0);
		_testGetName("SHA", null, "SHA-1", "MessageDigest", 0);
		_testGetName("SHA-512/256", null, "SHA-512/256", "MessageDigest", 0);
		_testGetName("SHA1withDSA", null, "DSA-SHA-1", "Signature", 0);
		_testGetName(
			"SHA256withECDSA", "P-256", "ECDSA-P-256-SHA-256", "Signature", 0);
		_testGetName(
			"SHA256withRSA", null, "RSA-PKCS1-1.5-SHA-256", "Signature", 0);
		_testGetName(
			"SHA256withRSA/PSS", null, "RSA-PSS-SHA-256", "Signature", 0);
		_testGetName(
			"SHA256withRSAandMGF1", null, "RSA-PSS-SHA-256", "Signature", 0);
		_testGetName("SHA3-256", null, "SHA3-256", "MessageDigest", 0);
		_testGetName("SHA384withECDSA", null, "ECDSA-SHA-384", "Signature", 0);
		_testGetName("TLSv1.3", null, "TLSv1.3", "SSLContext", 0);
		_testGetName("X25519", null, "x25519", "KeyAgreement", 0);

		String algorithm = RandomTestUtil.randomString();

		_testGetName(algorithm, null, algorithm, "Cipher", 0);
		_testGetName(algorithm, null, algorithm, "MessageDigest", 0);
		_testGetName(algorithm, null, algorithm, "Signature", 0);
	}

	private void _testGetName(
		String algorithm, String curve, String expectedName,
		String jcaClassName, int keySize) {

		Assert.assertEquals(
			expectedName,
			CryptoAlgorithmNameUtil.getName(
				algorithm, curve, jcaClassName, keySize));
	}

}