/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class FIPSModeLimitUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetJWSAlgorithms() {
		Assert.assertEquals(
			Arrays.asList(
				"ES256", "ES384", "ES512", "EdDSA", "PS256", "PS384", "PS512",
				"RS256", "RS384", "RS512"),
			FIPSModeLimitUtil.getJWSAlgorithms());
	}

	@Test
	public void testLimit() {
		List<String> descriptions = TransformUtil.transform(
			FIPSModeLimitUtil.limit(
				Arrays.asList(
					_createCryptoCall(
						"KeyGenerator", "company.encryption.algorithm"),
					_createCryptoCall("Mac", "passwords.encryption.algorithm"),
					_createCryptoCall(
						"MessageDigest", "auth.mac.algorithm",
						"passwords.encryption.algorithm.legacy"),
					_createCryptoCall(
						"MessageDigest", "passwords.encryption.algorithm"),
					_createCryptoCall(
						"TrustManagerFactory",
						"ssl.TrustManagerFactory.algorithm")),
				HashMapBuilder.<String, Set<String>>put(
					"auth.mac.algorithm", Collections.singleton("algorithm")
				).put(
					"company.encryption.algorithm",
					new TreeSet<>(Arrays.asList("algorithm", "key"))
				).put(
					"passwords.encryption.algorithm",
					Collections.singleton("pbkdf2")
				).build()),
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

		Assert.assertEquals(
			Arrays.asList(
				"KeyGenerator AES 128", "KeyGenerator AES 192",
				"KeyGenerator AES 256", "Mac HmacSHA256 0",
				"MessageDigest SHA-256 0", "MessageDigest SHA-384 0",
				"MessageDigest SHA-512 0",
				"MessageDigest configured by " +
					"[passwords.encryption.algorithm.legacy]",
				"TrustManagerFactory PKIX 0"),
			descriptions);
	}

	private CryptoCall _createCryptoCall(
		String jcaClassName, String... configuredBy) {

		CryptoCall cryptoCall = new CryptoCall(
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), jcaClassName);

		cryptoCall.setConfiguredBy(Arrays.asList(configuredBy));

		return cryptoCall;
	}

}