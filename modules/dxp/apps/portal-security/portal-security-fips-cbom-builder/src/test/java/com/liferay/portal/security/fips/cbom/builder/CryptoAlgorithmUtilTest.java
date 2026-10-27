/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import org.cyclonedx.model.component.crypto.enums.Primitive;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoAlgorithmUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetName() {
		_testGetName("AES", "AES", "Cipher", 0);
		_testGetName("AES", "AES-256", "KeyGenerator", 256);
		_testGetName("AES/CBC/PKCS5Padding", "AES-CBC", "Cipher", 0);
		_testGetName("AES/GCM/NoPadding", "AES-128-GCM", "Cipher", 128);
		_testGetName("AES/GCM/NoPadding", "AES-GCM", "Cipher", 0);
		_testGetName(
			"ChaCha20-Poly1305/None/NoPadding", "ChaCha20-Poly1305", "Cipher",
			0);
		_testGetName("DESede/CBC/PKCS5Padding", "3DES-CBC", "Cipher", 0);
		_testGetName("DH", "FFDH", "KeyAgreement", 0);
		_testGetName("Ed25519", "Ed25519", "Signature", 0);
		_testGetName("HmacSHA1", "HMAC-SHA-1", "Mac", 0);
		_testGetName("HmacSHA256", "HMAC-SHA-256", "KeyGenerator", 0);
		_testGetName("HmacSHA512", "HMAC-SHA-512", "Mac", 0);
		_testGetName("HmacSHA512", "HMAC-SHA-512", "SecretKeySpec", 0);
		_testGetName("MD5", "MD5", "MessageDigest", 0);
		_testGetName("PKCS12", "PKCS12", "KeyStore", 0);
		_testGetName("RAW", "RAW", "SecretKeySpec", 0);
		_testGetName("RSA", "RSA-2048", "KeyPairGenerator", 2048);
		_testGetName(
			"RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "RSA-OAEP-SHA-256",
			"Cipher", 0);
		_testGetName("RSA/ECB/PKCS1Padding", "RSA-PKCS1-1.5", "Cipher", 0);
		_testGetName("SHA", "SHA-1", "MessageDigest", 0);
		_testGetName("SHA-512/256", "SHA-512/256", "MessageDigest", 0);
		_testGetName("SHA1withDSA", "DSA-SHA-1", "Signature", 0);
		_testGetName("SHA256withRSA", "RSA-PKCS1-1.5-SHA-256", "Signature", 0);
		_testGetName("SHA256withRSA/PSS", "RSA-PSS-SHA-256", "Signature", 0);
		_testGetName("SHA3-256", "SHA3-256", "MessageDigest", 0);
		_testGetName("SHA384withECDSA", "ECDSA-SHA-384", "Signature", 0);
		_testGetName("TLSv1.3", "TLSv1.3", "SSLContext", 0);
		_testGetName("X25519", "x25519", "KeyAgreement", 0);

		String algorithm = RandomTestUtil.randomString();

		_testGetName(algorithm, algorithm, "Cipher", 0);
		_testGetName(algorithm, algorithm, "MessageDigest", 0);
		_testGetName(algorithm, algorithm, "Signature", 0);
	}

	@Test
	public void testGetPrimitive() {
		_testGetPrimitive(Primitive.AE, "AES/GCM/NoPadding", "Cipher");
		_testGetPrimitive(Primitive.AE, "ChaCha20-Poly1305", "Cipher");
		_testGetPrimitive(Primitive.BLOCK_CIPHER, "AES", "SecretKeySpec");
		_testGetPrimitive(
			Primitive.BLOCK_CIPHER, "AES/CBC/PKCS5Padding", "Cipher");
		_testGetPrimitive(
			Primitive.BLOCK_CIPHER, "ARIA/CBC/NoPadding", "Cipher");
		_testGetPrimitive(Primitive.BLOCK_CIPHER, "DESede", "KeyGenerator");
		_testGetPrimitive(Primitive.HASH, null, "MessageDigest");
		_testGetPrimitive(
			Primitive.KDF, "PBKDF2WithHmacSHA256", "SecretKeyFactory");
		_testGetPrimitive(Primitive.KEY_AGREE, "X25519", "KeyPairGenerator");
		_testGetPrimitive(Primitive.MAC, "HmacSHA256", "KeyGenerator");
		_testGetPrimitive(Primitive.OTHER, "PKCS12", "KeyStore");
		_testGetPrimitive(
			Primitive.PKE, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "Cipher");
		_testGetPrimitive(Primitive.SIGNATURE, "Ed25519", "KeyPairGenerator");
		_testGetPrimitive(
			Primitive.SIGNATURE, "RSASSA-PSS", "KeyPairGenerator");
		_testGetPrimitive(Primitive.SIGNATURE, "SHA256withRSA", "Signature");
		_testGetPrimitive(Primitive.STREAM_CIPHER, "ChaCha20", "Cipher");
		_testGetPrimitive(Primitive.UNKNOWN, "RSA", "KeyPairGenerator");
		_testGetPrimitive(Primitive.UNKNOWN, null, "Cipher");
	}

	private void _testGetName(
		String algorithm, String expectedName, String jcaClassName,
		int keySize) {

		Assert.assertEquals(
			expectedName,
			CryptoAlgorithmUtil.getName(algorithm, jcaClassName, keySize));
	}

	private void _testGetPrimitive(
		Primitive expectedPrimitive, String algorithm, String jcaClassName) {

		Assert.assertEquals(
			expectedPrimitive,
			CryptoAlgorithmUtil.getPrimitive(algorithm, jcaClassName));
	}

}