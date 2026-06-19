/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.fips;

import com.liferay.portal.kernel.util.PropsValues;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * @author Manuele Castro
 */
public class FIPSApprovedAlgorithms {

	/**
	 * Returns the FIPS approved algorithm names in the category, regardless of
	 * whether FIPS mode is enabled. Intended for filtering a consumer's own
	 * options while in FIPS mode; in non-FIPS mode a consumer should offer its
	 * own full list rather than calling this method.
	 */
	public static Set<String> getApprovedAlgorithms(
		FIPSAlgorithmCategory fipsAlgorithmCategory) {

		Set<String> algorithmNames = new HashSet<>();

		for (Map.Entry<String, Algorithm> entry : _algorithms.entrySet()) {
			Algorithm algorithm = entry.getValue();

			if (algorithm._fipsAlgorithmCategory == fipsAlgorithmCategory) {
				algorithmNames.add(entry.getKey());
			}
		}

		return algorithmNames;
	}

	public static boolean isApproved(String algorithmName) {
		if (!PropsValues.FIPS_ENABLED) {
			return true;
		}

		return _algorithms.containsKey(algorithmName);
	}

	public static boolean isApproved(String algorithmName, int keySize) {
		if (!PropsValues.FIPS_ENABLED) {
			return true;
		}

		Algorithm algorithm = _algorithms.get(algorithmName);

		if (algorithm == null) {
			return false;
		}

		IntPredicate keySizeValidIntPredicate =
			algorithm._keySizeValidIntPredicate;

		return keySizeValidIntPredicate.test(keySize);
	}

	private static Map<String, Algorithm> _createAlgorithms() {
		Set<Integer> aesKeySizes = Set.of(128, 192, 256);
		Set<Integer> ecKeySizes = Set.of(224, 256, 384, 521);

		IntPredicate aesKeySize = keySize -> aesKeySizes.contains(keySize);
		IntPredicate anyKeySize = keySize -> true;
		IntPredicate ecKeySize = keySize -> ecKeySizes.contains(keySize);
		IntPredicate pbkdf2KeySize = keySize -> keySize >= 112;
		IntPredicate rsaKeySize = keySize -> keySize >= 2048;

		Map<String, Algorithm> algorithms = new HashMap<>();

		algorithms.put(
			"SHA-1", new Algorithm(FIPSAlgorithmCategory.DIGEST, anyKeySize));
		algorithms.put(
			"SHA-256", new Algorithm(FIPSAlgorithmCategory.DIGEST, anyKeySize));
		algorithms.put(
			"SHA-384", new Algorithm(FIPSAlgorithmCategory.DIGEST, anyKeySize));
		algorithms.put(
			"SHA-512", new Algorithm(FIPSAlgorithmCategory.DIGEST, anyKeySize));

		algorithms.put(
			"PBKDF2WithHmacSHA1",
			new Algorithm(FIPSAlgorithmCategory.KDF, pbkdf2KeySize));
		algorithms.put(
			"PBKDF2WithHmacSHA256",
			new Algorithm(FIPSAlgorithmCategory.KDF, pbkdf2KeySize));

		algorithms.put(
			"HmacSHA1", new Algorithm(FIPSAlgorithmCategory.MAC, anyKeySize));
		algorithms.put(
			"HmacSHA256", new Algorithm(FIPSAlgorithmCategory.MAC, anyKeySize));
		algorithms.put(
			"HmacSHA384", new Algorithm(FIPSAlgorithmCategory.MAC, anyKeySize));

		algorithms.put(
			"AES",
			new Algorithm(
				FIPSAlgorithmCategory.SYMMETRIC_CIPHER, aesKeySize));

		algorithms.put(
			"RSA", new Algorithm(FIPSAlgorithmCategory.KEY_PAIR, rsaKeySize));
		algorithms.put(
			"EC", new Algorithm(FIPSAlgorithmCategory.KEY_PAIR, ecKeySize));

		algorithms.put(
			"SHA256withRSA",
			new Algorithm(FIPSAlgorithmCategory.SIGNATURE, anyKeySize));
		algorithms.put(
			"SHA384withRSA",
			new Algorithm(FIPSAlgorithmCategory.SIGNATURE, anyKeySize));
		algorithms.put(
			"SHA256withECDSA",
			new Algorithm(FIPSAlgorithmCategory.SIGNATURE, anyKeySize));

		return algorithms;
	}

	private static final Map<String, Algorithm> _algorithms =
		_createAlgorithms();

	private static class Algorithm {

		private Algorithm(
			FIPSAlgorithmCategory fipsAlgorithmCategory,
			IntPredicate keySizeValidIntPredicate) {

			_fipsAlgorithmCategory = fipsAlgorithmCategory;
			_keySizeValidIntPredicate = keySizeValidIntPredicate;
		}

		private final FIPSAlgorithmCategory _fipsAlgorithmCategory;
		private final IntPredicate _keySizeValidIntPredicate;

	}

}
