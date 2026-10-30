/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.fips.FIPSModeValidator;
import com.liferay.portal.kernel.util.StringUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Manuele Castro
 */
public class FIPSModeLimitUtil {

	public static List<String> getAllowedAlgorithms() {
		return _getSortedValues("_allowedAlgorithms");
	}

	public static Map<String, List<String>> getAllowedProviderNames() {
		Map<String, List<String>> allowedProviderNames = _getFieldValue(
			"_allowedProviderNames");

		return new TreeMap<>(allowedProviderNames);
	}

	public static List<Integer> getAllowedSymmetricKeySizes() {
		return _getSortedValues("_allowedSymmetricKeySizes");
	}

	public static List<String> getJWSAlgorithms() {
		return _getSortedValues("_allowedJWSAlgorithms");
	}

	public static int getPasswordsEncryptionKeySizeMin() {
		return _getFieldValue("_PASSWORDS_ENCRYPTION_ALGORITHM_KEY_SIZE_MIN");
	}

	public static int getPasswordsEncryptionRoundsMin() {
		return _getFieldValue("_PASSWORDS_ENCRYPTION_ALGORITHM_ROUNDS_MIN");
	}

	public static Map<String, String[]> getRequiredSecurityProperties() {
		Map<String, String[]> requiredSecurityProperties = _getFieldValue(
			"_requiredSecurityProperties");

		return new TreeMap<>(requiredSecurityProperties);
	}

	public static List<String> getTLSCipherSuites() {
		return _getSortedValues("_allowedTLSCipherSuites");
	}

	public static List<String> getTLSProtocols() {
		return _getSortedValues("_allowedTLSProtocols");
	}

	public static List<CryptoCall> limit(
		List<CryptoCall> cryptoCalls,
		Map<String, Set<String>> limitedSettings) {

		List<CryptoCall> limitedCryptoCalls = new ArrayList<>();

		Map<String, String[]> requiredSecurityProperties =
			getRequiredSecurityProperties();

		for (CryptoCall cryptoCall : cryptoCalls) {
			if (!cryptoCall.isConfigurable()) {
				limitedCryptoCalls.add(cryptoCall);

				continue;
			}

			Map<String, Set<String>> algorithmSettingNames = new TreeMap<>();
			Set<String> kinds = new TreeSet<>();
			List<String> unlimitedSettingKeys = new ArrayList<>();

			for (String settingKey : cryptoCall.getConfiguredBy()) {
				String[] requiredValues = requiredSecurityProperties.get(
					CryptoSettingUtil.getName(settingKey));

				if (requiredValues != null) {
					kinds.add("required");

					for (String requiredValue : requiredValues) {
						if (!_hasService(
								requiredValue, cryptoCall.getJCAClassName())) {

							continue;
						}

						Set<String> settingNames =
							algorithmSettingNames.computeIfAbsent(
								requiredValue, key -> new TreeSet<>());

						settingNames.add(CryptoSettingUtil.getName(settingKey));
					}

					continue;
				}

				Set<String> settingKinds = limitedSettings.get(settingKey);

				if (settingKinds == null) {
					unlimitedSettingKeys.add(settingKey);

					continue;
				}

				kinds.addAll(settingKinds);

				for (String algorithm :
						_getAllowedAlgorithms(
							cryptoCall.getJCAClassName(), settingKinds)) {

					Set<String> settingNames =
						algorithmSettingNames.computeIfAbsent(
							algorithm, key -> new TreeSet<>());

					settingNames.add(CryptoSettingUtil.getName(settingKey));
				}
			}

			for (Map.Entry<String, Set<String>> entry :
					algorithmSettingNames.entrySet()) {

				limitedCryptoCalls.addAll(
					_getLimitedCryptoCalls(
						entry.getKey(), cryptoCall, kinds, entry.getValue()));
			}

			if (unlimitedSettingKeys.isEmpty()) {
				continue;
			}

			CryptoCall unlimitedCryptoCall = cryptoCall.copy();

			unlimitedCryptoCall.setConfiguredBy(unlimitedSettingKeys);

			limitedCryptoCalls.add(unlimitedCryptoCall);
		}

		return limitedCryptoCalls;
	}

	private static Set<String> _getAllowedAlgorithms(
		String jcaClassName, Set<String> kinds) {

		Set<String> allowedAlgorithms = null;

		for (String kind : kinds) {
			Set<String> kindAllowedAlgorithms = new TreeSet<>();

			for (String algorithm : getAllowedAlgorithms()) {
				if (Objects.equals(kind, "pbkdf2")) {
					String pbkdf2Algorithm = _getPBKDF2Algorithm(
						algorithm, jcaClassName);

					if (pbkdf2Algorithm != null) {
						kindAllowedAlgorithms.add(pbkdf2Algorithm);
					}

					continue;
				}

				if (FIPSModeValidator.isNotAllowedAlgorithm(algorithm) ||
					(Objects.equals(kind, "key") &&
					 !_hasService(algorithm, "Cipher"))) {

					continue;
				}

				if (Objects.equals(jcaClassName, "SecretKeySpec")) {
					if (_hasService(algorithm, "Cipher") ||
						_hasService(algorithm, "Mac")) {

						kindAllowedAlgorithms.add(algorithm);
					}

					continue;
				}

				if (!_hasService(algorithm, jcaClassName)) {
					continue;
				}

				kindAllowedAlgorithms.add(algorithm);
			}

			if (allowedAlgorithms == null) {
				allowedAlgorithms = kindAllowedAlgorithms;
			}
			else {
				allowedAlgorithms.retainAll(kindAllowedAlgorithms);
			}
		}

		if (allowedAlgorithms == null) {
			return Collections.emptySet();
		}

		return allowedAlgorithms;
	}

	private static <T> T _getFieldValue(String name) {
		try {
			Field field = ReflectionUtil.getDeclaredField(
				FIPSModeValidator.class, name);

			return (T)field.get(null);
		}
		catch (Exception exception) {
			throw new IllegalStateException(
				"Unable to read FIPSModeValidator's " + name, exception);
		}
	}

	private static List<CryptoCall> _getLimitedCryptoCalls(
		String algorithm, CryptoCall cryptoCall, Set<String> kinds,
		Set<String> settingNames) {

		List<Integer> keySizes = Collections.singletonList(
			cryptoCall.getKeySize());

		if ((cryptoCall.getKeySize() <= 0) && kinds.contains("key")) {
			keySizes = getAllowedSymmetricKeySizes();
		}

		String note = StringBundler.concat(
			"FIPS mode limits ", StringUtil.merge(settingNames, ", "),
			" to the algorithms FIPSModeValidator allows");

		if (kinds.contains("required")) {
			note = StringBundler.concat(
				"FIPS mode requires ", StringUtil.merge(settingNames, ", "),
				" to include ", algorithm);
		}

		if (kinds.contains("pbkdf2")) {
			note = StringBundler.concat(
				note, ", with at least ", getPasswordsEncryptionRoundsMin(),
				" iterations and a ", getPasswordsEncryptionKeySizeMin(),
				"-bit output for PBKDF2");
		}

		List<CryptoCall> cryptoCalls = new ArrayList<>();

		for (int keySize : keySizes) {
			CryptoCall limitedCryptoCall = cryptoCall.copy();

			limitedCryptoCall.setAlgorithm(
				CryptoProviderUtil.getAlgorithmName(
					algorithm, cryptoCall.getJCAClassName()));
			limitedCryptoCall.setConfiguredBy(Collections.emptyList());
			limitedCryptoCall.setKeySize(keySize);
			limitedCryptoCall.setNote(note + StringPool.PERIOD);

			cryptoCalls.add(limitedCryptoCall);
		}

		return cryptoCalls;
	}

	private static String _getPBKDF2Algorithm(
		String algorithm, String jcaClassName) {

		if (!algorithm.startsWith("PBKDF2WithHmac")) {
			return null;
		}

		try {
			Method method = ReflectionUtil.getDeclaredMethod(
				FIPSModeValidator.class,
				"_validatePasswordsEncryptionAlgorithm", String.class);

			method.invoke(
				null,
				StringBundler.concat(
					algorithm, StringPool.SLASH,
					getPasswordsEncryptionKeySizeMin(), StringPool.SLASH,
					getPasswordsEncryptionRoundsMin()));
		}
		catch (InvocationTargetException invocationTargetException) {
			if (_log.isDebugEnabled()) {
				_log.debug(invocationTargetException);
			}

			return null;
		}
		catch (Exception exception) {
			throw new IllegalStateException(
				"Unable to call FIPSModeValidator's PBKDF2 check", exception);
		}

		if (Objects.equals(jcaClassName, "SecretKeyFactory")) {
			return algorithm;
		}

		if (Objects.equals(jcaClassName, "Mac") ||
			Objects.equals(jcaClassName, "SecretKeySpec")) {

			return "Hmac" + algorithm.substring("PBKDF2WithHmac".length());
		}

		return null;
	}

	private static <T> List<T> _getSortedValues(String name) {
		Collection<T> values = _getFieldValue(name);

		return new ArrayList<>(new TreeSet<>(values));
	}

	private static boolean _hasService(String algorithm, String jcaClassName) {
		if (CryptoProviderUtil.fetchProviderName(algorithm, jcaClassName) !=
				null) {

			return true;
		}

		return false;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FIPSModeLimitUtil.class);

}