/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.cyclonedx.model.component.crypto.enums.Primitive;

/**
 * @author Manuele Castro
 */
public class CryptoAlgorithmUtil {

	public static String getName(
		String algorithm, String jcaClassName, int keySize) {

		String name = _getName(algorithm, jcaClassName);

		if (keySize <= 0) {
			return name;
		}

		if (name.equals("AES") || name.startsWith("AES-")) {
			return "AES-" + keySize + name.substring(3);
		}

		if (Objects.equals(jcaClassName, "KeyPairGenerator")) {
			return name + StringPool.DASH + keySize;
		}

		return name;
	}

	public static Primitive getPrimitive(
		String algorithm, String jcaClassName) {

		Primitive primitive = _jcaClassPrimitives.get(jcaClassName);

		if (primitive != null) {
			return primitive;
		}

		if (algorithm == null) {
			return Primitive.UNKNOWN;
		}

		String[] algorithmParts = StringUtil.split(
			StringUtil.toUpperCase(algorithm), CharPool.SLASH);

		String name = algorithmParts[0];

		if (name.startsWith("HMAC")) {
			return Primitive.MAC;
		}

		if (name.startsWith("PBE") || name.startsWith("PBKDF2")) {
			return Primitive.KDF;
		}

		if (name.startsWith("AES")) {
			name = "AES";
		}

		primitive = _cipherPrimitives.get(
			_cipherNames.getOrDefault(name, name));

		if ((primitive == Primitive.BLOCK_CIPHER) &&
			Objects.equals(jcaClassName, "Cipher") &&
			(algorithmParts.length > 1) &&
			_aeModeNames.contains(algorithmParts[1])) {

			return Primitive.AE;
		}

		if (primitive != null) {
			return primitive;
		}

		if (name.equals("RSA") && Objects.equals(jcaClassName, "Cipher")) {
			return Primitive.PKE;
		}

		if (_keyAgreementNames.containsKey(name)) {
			return Primitive.KEY_AGREE;
		}

		if (_signatureNames.containsKey(name)) {
			return Primitive.SIGNATURE;
		}

		return Primitive.UNKNOWN;
	}

	private static String _getCipherName(String algorithm) {
		String[] transformationParts = StringUtil.split(
			StringUtil.toUpperCase(algorithm), CharPool.SLASH);

		String name = _cipherNames.get(transformationParts[0]);

		if (name == null) {
			return algorithm;
		}

		if (name.equals("RSA")) {
			if (transformationParts.length != 3) {
				return algorithm;
			}

			return _getRSACipherName(algorithm, transformationParts[2]);
		}

		if ((transformationParts.length == 1) ||
			transformationParts[1].equals("NONE")) {

			return name;
		}

		return name + StringPool.DASH + transformationParts[1];
	}

	private static String _getName(String algorithm, String jcaClassName) {
		if (Objects.equals(jcaClassName, "Cipher")) {
			return _getCipherName(algorithm);
		}

		String upperCaseAlgorithm = StringUtil.toUpperCase(algorithm);

		if (Objects.equals(jcaClassName, "KeyAgreement")) {
			return _keyAgreementNames.getOrDefault(
				upperCaseAlgorithm, algorithm);
		}

		if (Objects.equals(jcaClassName, "MessageDigest")) {
			return _digestNames.getOrDefault(upperCaseAlgorithm, algorithm);
		}

		if (Objects.equals(jcaClassName, "Signature")) {
			return _getSignatureName(algorithm, upperCaseAlgorithm);
		}

		if (upperCaseAlgorithm.startsWith("HMAC")) {
			String digestName = _digestNames.get(
				upperCaseAlgorithm.substring(4));

			if (digestName != null) {
				return "HMAC-" + digestName;
			}
		}

		return _cipherNames.getOrDefault(upperCaseAlgorithm, algorithm);
	}

	private static String _getRSACipherName(
		String algorithm, String paddingName) {

		if (paddingName.equals("OAEPPADDING")) {
			return "RSA-OAEP";
		}

		if (paddingName.equals("PKCS1PADDING")) {
			return "RSA-PKCS1-1.5";
		}

		if (paddingName.startsWith("OAEPWITH") &&
			paddingName.endsWith("ANDMGF1PADDING")) {

			String digestName = _digestNames.get(
				StringUtil.removeSubstrings(
					paddingName, "OAEPWITH", "ANDMGF1PADDING"));

			if (digestName != null) {
				return "RSA-OAEP-" + digestName;
			}
		}

		return algorithm;
	}

	private static String _getSignatureName(
		String algorithm, String upperCaseAlgorithm) {

		int index = upperCaseAlgorithm.indexOf("WITH");

		if (index == -1) {
			return _signatureNames.getOrDefault(upperCaseAlgorithm, algorithm);
		}

		String digestName = _digestNames.get(
			upperCaseAlgorithm.substring(0, index));
		String prefix = _signaturePrefixes.get(
			upperCaseAlgorithm.substring(index + 4));

		if ((digestName == null) || (prefix == null)) {
			return algorithm;
		}

		return prefix + StringPool.DASH + digestName;
	}

	private static final Set<String> _aeModeNames = SetUtil.fromArray(
		"CCM", "GCM");
	private static final Map<String, String> _cipherNames = HashMapBuilder.put(
		"AES", "AES"
	).put(
		"ARCFOUR", "RC4"
	).put(
		"BLOWFISH", "Blowfish"
	).put(
		"CHACHA20", "ChaCha20"
	).put(
		"CHACHA20-POLY1305", "ChaCha20-Poly1305"
	).put(
		"DES", "DES"
	).put(
		"DESEDE", "3DES"
	).put(
		"RC4", "RC4"
	).put(
		"RSA", "RSA"
	).put(
		"TRIPLEDES", "3DES"
	).build();
	private static final Map<String, Primitive> _cipherPrimitives =
		HashMapBuilder.put(
			"3DES", Primitive.BLOCK_CIPHER
		).put(
			"AES", Primitive.BLOCK_CIPHER
		).put(
			"ARIA", Primitive.BLOCK_CIPHER
		).put(
			"Blowfish", Primitive.BLOCK_CIPHER
		).put(
			"CAMELLIA", Primitive.BLOCK_CIPHER
		).put(
			"ChaCha20", Primitive.STREAM_CIPHER
		).put(
			"ChaCha20-Poly1305", Primitive.AE
		).put(
			"DES", Primitive.BLOCK_CIPHER
		).put(
			"RC2", Primitive.BLOCK_CIPHER
		).put(
			"RC4", Primitive.STREAM_CIPHER
		).put(
			"SM4", Primitive.BLOCK_CIPHER
		).build();
	private static final Map<String, String> _digestNames = HashMapBuilder.put(
		"MD2", "MD2"
	).put(
		"MD4", "MD4"
	).put(
		"MD5", "MD5"
	).put(
		"SHA", "SHA-1"
	).put(
		"SHA-1", "SHA-1"
	).put(
		"SHA-224", "SHA-224"
	).put(
		"SHA-256", "SHA-256"
	).put(
		"SHA-384", "SHA-384"
	).put(
		"SHA-512", "SHA-512"
	).put(
		"SHA-512/224", "SHA-512/224"
	).put(
		"SHA-512/256", "SHA-512/256"
	).put(
		"SHA1", "SHA-1"
	).put(
		"SHA3-224", "SHA3-224"
	).put(
		"SHA3-256", "SHA3-256"
	).put(
		"SHA3-384", "SHA3-384"
	).put(
		"SHA3-512", "SHA3-512"
	).put(
		"SHA224", "SHA-224"
	).put(
		"SHA256", "SHA-256"
	).put(
		"SHA384", "SHA-384"
	).put(
		"SHA512", "SHA-512"
	).put(
		"SHA512/224", "SHA-512/224"
	).put(
		"SHA512/256", "SHA-512/256"
	).build();
	private static final Map<String, Primitive> _jcaClassPrimitives =
		HashMapBuilder.put(
			"KeyAgreement", Primitive.KEY_AGREE
		).put(
			"KeyManagerFactory", Primitive.OTHER
		).put(
			"KeyStore", Primitive.OTHER
		).put(
			"Mac", Primitive.MAC
		).put(
			"MessageDigest", Primitive.HASH
		).put(
			"Signature", Primitive.SIGNATURE
		).put(
			"SSLContext", Primitive.OTHER
		).put(
			"TrustManagerFactory", Primitive.OTHER
		).build();
	private static final Map<String, String> _keyAgreementNames =
		HashMapBuilder.put(
			"DH", "FFDH"
		).put(
			"DIFFIEHELLMAN", "FFDH"
		).put(
			"ECDH", "ECDH"
		).put(
			"X448", "x448"
		).put(
			"X25519", "x25519"
		).put(
			"XDH", "XDH"
		).build();
	private static final Map<String, String> _signatureNames =
		HashMapBuilder.put(
			"DSA", "DSA"
		).put(
			"ECDSA", "ECDSA"
		).put(
			"ED448", "Ed448"
		).put(
			"ED25519", "Ed25519"
		).put(
			"EDDSA", "EdDSA"
		).put(
			"RSASSA-PSS", "RSA-PSS"
		).build();
	private static final Map<String, String> _signaturePrefixes =
		HashMapBuilder.put(
			"DSA", "DSA"
		).put(
			"ECDSA", "ECDSA"
		).put(
			"RSA", "RSA-PKCS1-1.5"
		).put(
			"RSA/PSS", "RSA-PSS"
		).build();

}