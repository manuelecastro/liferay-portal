/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;

/**
 * @author Manuele Castro
 */
public class CryptoEnvironmentUtil {

	public static List<CryptoCall> getCipherSuiteCryptoCalls(
		String cipherSuiteName, CryptoCall cryptoCall) {

		String name = StringUtil.removeFirst(cipherSuiteName, "TLS_");

		int index = name.lastIndexOf(CharPool.UNDERLINE);

		String hashName = name.substring(index + 1);

		name = name.substring(0, index);

		index = name.indexOf("_WITH_");

		String cipherName = name;

		if (index != -1) {
			cipherName = name.substring(index + 6);
		}

		String[] cipherParts = StringUtil.split(cipherName, CharPool.UNDERLINE);

		List<CryptoCall> cryptoCalls = new ArrayList<>();

		cryptoCalls.add(
			_createCryptoCall(
				StringBundler.concat(
					cipherParts[0], StringPool.SLASH, cipherParts[2],
					"/NoPadding"),
				cryptoCall.getClassName(),
				Arrays.asList(CryptoFunction.DECRYPT, CryptoFunction.ENCRYPT),
				"Cipher", GetterUtil.getInteger(cipherParts[1]), cryptoCall,
				_NOTE_CIPHER_SUITE));

		if (index == -1) {
			cryptoCalls.add(
				_createCryptoCall(
					"SHA-" + hashName.substring(3), cryptoCall.getClassName(),
					Collections.singletonList(CryptoFunction.DIGEST),
					"MessageDigest", 0, cryptoCall, _NOTE_CIPHER_SUITE));

			return cryptoCalls;
		}

		String[] exchangeParts = StringUtil.split(
			name.substring(0, index), CharPool.UNDERLINE);

		cryptoCalls.add(
			_createCryptoCall(
				StringUtil.removeLast(exchangeParts[0], "E"),
				cryptoCall.getClassName(),
				Collections.singletonList(CryptoFunction.KEYDERIVE),
				"KeyAgreement", 0, cryptoCall, _NOTE_CIPHER_SUITE));
		cryptoCalls.add(
			_createCryptoCall(
				hashName + "with" + exchangeParts[1], cryptoCall.getClassName(),
				Arrays.asList(CryptoFunction.SIGN, CryptoFunction.VERIFY),
				"Signature", 0, cryptoCall, _NOTE_CIPHER_SUITE));

		return cryptoCalls;
	}

	public static List<CryptoCall> getCryptoCalls(
		List<CryptoCall> cryptoCalls, boolean fipsEnabled) {

		CryptoCall clusterCryptoCall = null;
		Map<String, CryptoCall> tlsCryptoCalls = new TreeMap<>();

		for (CryptoCall cryptoCall : cryptoCalls) {
			String className = cryptoCall.getClassName();

			if ((clusterCryptoCall == null) && className.contains("JGroups")) {
				clusterCryptoCall = cryptoCall;
			}

			if (!Objects.equals(cryptoCall.getJCAClassName(), "SSLContext")) {
				continue;
			}

			tlsCryptoCalls.putIfAbsent(className, cryptoCall);
		}

		List<CryptoCall> environmentCryptoCalls = new ArrayList<>();

		for (CryptoCall tlsCryptoCall : tlsCryptoCalls.values()) {
			if (fipsEnabled) {
				_addFIPSTLSCryptoCalls(environmentCryptoCalls, tlsCryptoCall);
			}
			else {
				environmentCryptoCalls.add(
					_createConfigurableCryptoCall(
						tlsCryptoCall.getClassName(),
						Arrays.asList(
							CryptoSettingUtil.toSettingKey(
								"system properties",
								"jdk.tls.client.protocols"),
							CryptoSettingUtil.toSettingKey(
								"system properties", "https.protocols"),
							CryptoSettingUtil.toSettingKey(
								"java.security", "jdk.tls.disabledAlgorithms")),
						Collections.singletonList(CryptoFunction.OTHER),
						"SSLContext", tlsCryptoCall, _NOTE_TLS));
			}
		}

		if (clusterCryptoCall != null) {
			if (fipsEnabled) {
				_addFIPSClusterCryptoCalls(
					clusterCryptoCall, environmentCryptoCalls);
			}
			else {
				environmentCryptoCalls.add(
					_createConfigurableCryptoCall(
						clusterCryptoCall.getClassName(),
						Arrays.asList(
							"cluster.link.channel.properties.control",
							"cluster.link.channel.properties.transport.0"),
						Arrays.asList(
							CryptoFunction.DECRYPT, CryptoFunction.ENCRYPT),
						"Cipher", clusterCryptoCall, _NOTE_CLUSTER));
			}
		}

		return environmentCryptoCalls;
	}

	private static void _addFIPSClusterCryptoCalls(
		CryptoCall clusterCryptoCall, List<CryptoCall> cryptoCalls) {

		for (int keySize : FIPSModeLimitUtil.getAllowedSymmetricKeySizes()) {
			cryptoCalls.add(
				_createCryptoCall(
					"AES/CBC/PKCS5Padding", clusterCryptoCall.getClassName(),
					Arrays.asList(
						CryptoFunction.DECRYPT, CryptoFunction.ENCRYPT),
					"Cipher", keySize, clusterCryptoCall, _NOTE_FIPS_CLUSTER));
		}

		cryptoCalls.add(
			_createCryptoCall(
				"PKCS12", clusterCryptoCall.getClassName(),
				Collections.singletonList(CryptoFunction.OTHER), "KeyStore", 0,
				clusterCryptoCall,
				"The keystore of the X509Token member check of a JGroups " +
					"channel, which FIPS mode requires to be PKCS12 through " +
						"cluster.link.auth.keystore.type."));
		cryptoCalls.add(
			_createConfigurableCryptoCall(
				clusterCryptoCall.getClassName(),
				Collections.singletonList("cluster.link.auth.cipher.type"),
				Arrays.asList(CryptoFunction.DECRYPT, CryptoFunction.ENCRYPT),
				"Cipher", clusterCryptoCall,
				"The cipher of the X509Token member check of a JGroups " +
					"channel, which FIPS mode does not limit."));
	}

	private static void _addFIPSTLSCryptoCalls(
		List<CryptoCall> cryptoCalls, CryptoCall tlsCryptoCall) {

		Map<String, String[]> requiredSecurityProperties =
			FIPSModeLimitUtil.getRequiredSecurityProperties();

		String exclusions = StringUtil.merge(
			requiredSecurityProperties.get("jdk.tls.disabledAlgorithms"), ", ");

		for (String protocol : FIPSModeLimitUtil.getTLSProtocols()) {
			List<String> cipherSuites = new ArrayList<>();

			for (String cipherSuite : FIPSModeLimitUtil.getTLSCipherSuites()) {
				if (cipherSuite.contains("_WITH_") != Objects.equals(
						protocol, "TLSv1.2")) {

					continue;
				}

				cipherSuites.add(cipherSuite);
			}

			CryptoCall cryptoCall = _createCryptoCall(
				protocol, tlsCryptoCall.getClassName(),
				Collections.singletonList(CryptoFunction.OTHER), "SSLContext",
				0, tlsCryptoCall,
				StringBundler.concat(
					"FIPS mode allows ", protocol,
					" with these cipher suites (FIPSModeValidator). It ",
					"excludes ", exclusions, ", which ",
					"jdk.tls.disabledAlgorithms must disable at startup, and ",
					"every other cipher suite. This covers the connections ",
					"DXP filters through FIPSModeValidator's lists. Other ",
					"outbound connections follow the JVM's TLS settings, ",
					"which FIPS mode constrains at startup through ",
					"jdk.tls.client.protocols and jdk.tls.disabledAlgorithms. ",
					"TLS that a load balancer or a reverse proxy terminates ",
					"is not covered."));

			cryptoCall.setCipherSuites(cipherSuites);

			cryptoCalls.add(cryptoCall);

			for (String cipherSuite : cipherSuites) {
				cryptoCalls.addAll(
					getCipherSuiteCryptoCalls(cipherSuite, cryptoCall));
			}
		}
	}

	private static CryptoCall _createConfigurableCryptoCall(
		String className, List<String> configuredBy,
		List<CryptoFunction> cryptoFunctions, String jcaClassName,
		CryptoCall ownerCryptoCall, String note) {

		CryptoCall cryptoCall = _createCryptoCall(
			null, className, cryptoFunctions, jcaClassName, 0, ownerCryptoCall,
			note);

		cryptoCall.setConfiguredBy(configuredBy);

		return cryptoCall;
	}

	private static CryptoCall _createCryptoCall(
		String algorithm, String className,
		List<CryptoFunction> cryptoFunctions, String jcaClassName, int keySize,
		CryptoCall ownerCryptoCall, String note) {

		CryptoCall cryptoCall = new CryptoCall(
			ownerCryptoCall.getBundleSymbolicName(), className,
			ownerCryptoCall.getFeatureName(), jcaClassName);

		if (algorithm != null) {
			cryptoCall.setAlgorithm(
				CryptoProviderUtil.getAlgorithmName(algorithm, jcaClassName));
		}

		cryptoCall.setCryptoFunctions(cryptoFunctions);
		cryptoCall.setKeySize(keySize);
		cryptoCall.setNote(note);

		return cryptoCall;
	}

	private static final String _NOTE_CIPHER_SUITE =
		"An algorithm of the TLS cipher suites that FIPS mode allows.";

	private static final String _NOTE_CLUSTER = StringBundler.concat(
		"With FIPS mode off, the JGroups channel configuration chooses ",
		"whether and how DXP nodes encrypt the traffic between them.");

	private static final String _NOTE_FIPS_CLUSTER = StringBundler.concat(
		"JGroups SYM_ENCRYPT between DXP nodes, with a 16-byte IV, and a FIPS ",
		"provider when the channel names one. FIPS mode requires it of a ",
		"channel that encrypts, when cluster.link.enabled is on.");

	private static final String _NOTE_TLS = StringBundler.concat(
		"With FIPS mode off, nothing in DXP limits the TLS versions or cipher ",
		"suites. The JVM's settings choose them, so the CBOM claims no ",
		"exclusions.");

}