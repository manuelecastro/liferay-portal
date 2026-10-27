/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoEnvironmentUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetCipherSuiteCryptoCalls() {
		CryptoCall cryptoCall = _createCryptoCall(
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			"SSLContext");

		Assert.assertEquals(
			Arrays.asList(
				"Cipher AES/GCM/NoPadding 128", "MessageDigest SHA-256 0"),
			_getDescriptions(
				CryptoEnvironmentUtil.getCipherSuiteCryptoCalls(
					"TLS_AES_128_GCM_SHA256", cryptoCall)));
		Assert.assertEquals(
			Arrays.asList(
				"Cipher AES/GCM/NoPadding 256", "KeyAgreement ECDH 0",
				"Signature SHA384withRSA 0"),
			_getDescriptions(
				CryptoEnvironmentUtil.getCipherSuiteCryptoCalls(
					"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", cryptoCall)));
	}

	@Test
	public void testGetCryptoCalls() throws Exception {
		List<CryptoCall> cryptoCalls = Arrays.asList(
			_createCryptoCall(
				"com.liferay.portal.kernel", "com.liferay.test.TLSClass",
				"SSLContext"),
			_createCryptoCall(
				RandomTestUtil.randomString(), "com.liferay.test.JGroupsClass",
				"SecretKeySpec"));

		Assert.assertEquals(
			Arrays.asList(
				"Cipher configured by [cluster.link.channel.properties." +
					"control, cluster.link.channel.properties.transport.0]",
				"SSLContext configured by [jdk.tls.client.protocols@system " +
					"properties, https.protocols@system properties, " +
						"jdk.tls.disabledAlgorithms@java.security]",
				"SecureRandom configured by [java.security.egd@system " +
					"properties, securerandom.source@java.security]"),
			_getDescriptions(
				CryptoEnvironmentUtil.getCryptoCalls(
					CryptoAssessment.load(null, null), cryptoCalls, false)));

		List<CryptoCall> fipsCryptoCalls = CryptoEnvironmentUtil.getCryptoCalls(
			CryptoAssessment.load(null, null), cryptoCalls, true);

		List<String> descriptions = _getDescriptions(fipsCryptoCalls);

		Assert.assertTrue(
			descriptions.containsAll(
				Arrays.asList(
					"Cipher AES/CBC/PKCS5Padding 128",
					"Cipher AES/CBC/PKCS5Padding 192",
					"Cipher AES/CBC/PKCS5Padding 256",
					"Cipher configured by [cluster.link.auth.cipher.type]",
					"KeyStore PKCS12 0", "SSLContext TLSv1.2 0",
					"SSLContext TLSv1.3 0")));

		for (CryptoCall cryptoCall : fipsCryptoCalls) {
			if (!Objects.equals(cryptoCall.getAlgorithm(), "TLSv1.2")) {
				continue;
			}

			Assert.assertEquals(
				Arrays.asList(
					"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
					"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
					"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
					"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"),
				cryptoCall.getCipherSuites());
		}
	}

	private CryptoCall _createCryptoCall(
		String bundleSymbolicName, String className, String jcaClassName) {

		return new CryptoCall(
			bundleSymbolicName, className, RandomTestUtil.randomString(),
			jcaClassName);
	}

	private List<String> _getDescriptions(List<CryptoCall> cryptoCalls) {
		List<String> descriptions = TransformUtil.transform(
			cryptoCalls,
			cryptoCall -> {
				if (cryptoCall.isConfigurable()) {
					return cryptoCall.getJCAClassName() + " configured by " +
						cryptoCall.getConfiguredBy();
				}

				return StringBundler.concat(
					cryptoCall.getJCAClassName(), StringPool.SPACE,
					cryptoCall.getAlgorithm(), StringPool.SPACE,
					cryptoCall.getKeySize());
			});

		descriptions.sort(null);

		return descriptions;
	}

}