/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.InputStream;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoUsageDataUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		jsonFactoryUtil.setJSONFactory(new JSONFactoryImpl());
	}

	@Test
	public void testGetCryptoUsages() throws Exception {
		String className1 = RandomTestUtil.randomString();
		String className2 = RandomTestUtil.randomString();
		String featureName1 = RandomTestUtil.randomString();
		String featureName2 = RandomTestUtil.randomString();
		String propertyName = RandomTestUtil.randomString();

		List<CryptoUsage> cryptoUsages = CryptoUsageDataUtil.getCryptoUsages(
			JSONUtil.put(
				"cryptoUsages",
				JSONUtil.putAll(
					JSONUtil.put(
						"algorithm", "AES/GCM/NoPadding"
					).put(
						"classNames", JSONUtil.putAll(className1)
					).put(
						"cryptoFunctions", JSONUtil.putAll("decrypt", "encrypt")
					).put(
						"cryptoPrimitive", "ae"
					).put(
						"featureName", featureName1
					).put(
						"jcaClassName", "Cipher"
					).put(
						"keySize", 256
					),
					JSONUtil.put(
						"classNames", JSONUtil.putAll(className2)
					).put(
						"configuredBy", JSONUtil.putAll(propertyName)
					).put(
						"cryptoFunctions", JSONUtil.putAll("keygen")
					).put(
						"cryptoPrimitive", "unknown"
					).put(
						"curve", "P-256"
					).put(
						"featureName", featureName2
					).put(
						"jcaClassName", "KeyGenerator"
					))));

		Assert.assertEquals(cryptoUsages.toString(), 2, cryptoUsages.size());

		CryptoUsage cryptoUsage = cryptoUsages.get(0);

		Assert.assertEquals("AES/GCM/NoPadding", cryptoUsage.getAlgorithm());
		Assert.assertEquals(
			Collections.singletonList(className1), cryptoUsage.getClassNames());
		Assert.assertEquals(
			Collections.emptyList(), cryptoUsage.getConfiguredBy());
		Assert.assertEquals(
			Arrays.asList(CryptoFunction.DECRYPT, CryptoFunction.ENCRYPT),
			cryptoUsage.getCryptoFunctions());
		Assert.assertEquals(
			CryptoPrimitive.AE, cryptoUsage.getCryptoPrimitive());
		Assert.assertNull(cryptoUsage.getCurve());
		Assert.assertEquals(featureName1, cryptoUsage.getFeatureName());
		Assert.assertEquals("Cipher", cryptoUsage.getJCAClassName());
		Assert.assertEquals(256, cryptoUsage.getKeySize());
		Assert.assertFalse(cryptoUsage.isConfigurable());

		cryptoUsage = cryptoUsages.get(1);

		Assert.assertNull(cryptoUsage.getAlgorithm());
		Assert.assertEquals(
			Collections.singletonList(className2), cryptoUsage.getClassNames());
		Assert.assertEquals(
			Collections.singletonList(propertyName),
			cryptoUsage.getConfiguredBy());
		Assert.assertEquals(
			Collections.singletonList(CryptoFunction.KEYGEN),
			cryptoUsage.getCryptoFunctions());
		Assert.assertEquals(
			CryptoPrimitive.UNKNOWN, cryptoUsage.getCryptoPrimitive());
		Assert.assertEquals("P-256", cryptoUsage.getCurve());
		Assert.assertEquals(featureName2, cryptoUsage.getFeatureName());
		Assert.assertEquals("KeyGenerator", cryptoUsage.getJCAClassName());
		Assert.assertEquals(0, cryptoUsage.getKeySize());
		Assert.assertTrue(cryptoUsage.isConfigurable());
	}

	@Test
	public void testGetCryptoUsagesWithInvalidData() throws Exception {

		// Feature name

		JSONObject jsonObject = _createCryptoUsageJSONObject();

		jsonObject.remove("featureName");

		_testGetCryptoUsagesWithInvalidData(
			"A crypto usage has no feature name", jsonObject);

		// Class names

		jsonObject = _createCryptoUsageJSONObject();

		jsonObject.remove("classNames");

		_testGetCryptoUsagesWithInvalidData(
			"The crypto usage of " + jsonObject.getString("featureName") +
				" has no class names",
			jsonObject);

		// Crypto functions

		jsonObject = _createCryptoUsageJSONObject();

		jsonObject.remove("cryptoFunctions");

		_testGetCryptoUsagesWithInvalidData(
			"The crypto usage of " + jsonObject.getString("featureName") +
				" has no crypto functions",
			jsonObject);

		jsonObject = _createCryptoUsageJSONObject();

		jsonObject.put("cryptoFunctions", JSONUtil.putAll("decipher"));

		_testGetCryptoUsagesWithInvalidData(
			"Invalid crypto function decipher", jsonObject);

		// Crypto primitive

		jsonObject = _createCryptoUsageJSONObject();

		jsonObject.put("cryptoPrimitive", "cipher");

		_testGetCryptoUsagesWithInvalidData(
			"Invalid crypto primitive cipher", jsonObject);

		// JCA class name

		jsonObject = _createCryptoUsageJSONObject();

		jsonObject.put("jcaClassName", "SecureRandom");

		_testGetCryptoUsagesWithInvalidData(
			StringBundler.concat(
				"The crypto usage of ", jsonObject.getString("featureName"),
				" has an unsupported JCA class SecureRandom"),
			jsonObject);

		// Algorithm and configured by

		jsonObject = _createCryptoUsageJSONObject();

		jsonObject.put(
			"configuredBy", JSONUtil.putAll(RandomTestUtil.randomString()));

		_testGetCryptoUsagesWithInvalidData(
			StringBundler.concat(
				"The crypto usage of ", jsonObject.getString("featureName"),
				" needs either an algorithm or the portal properties that ",
				"configure it"),
			jsonObject);

		jsonObject = _createCryptoUsageJSONObject();

		jsonObject.remove("algorithm");

		_testGetCryptoUsagesWithInvalidData(
			StringBundler.concat(
				"The crypto usage of ", jsonObject.getString("featureName"),
				" needs either an algorithm or the portal properties that ",
				"configure it"),
			jsonObject);
	}

	@Test
	public void testGetCryptoUsagesWithShippedData() throws Exception {
		List<CryptoUsage> cryptoUsages = CryptoUsageDataUtil.getCryptoUsages(
			JSONFactoryUtil.createJSONObject(
				_read("dependencies/crypto-usages.json")));

		Assert.assertFalse(cryptoUsages.isEmpty());

		String portalPropertiesString = _read("/portal.properties");

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			for (String propertyName : cryptoUsage.getConfiguredBy()) {
				Assert.assertTrue(
					propertyName,
					portalPropertiesString.contains(
						propertyName + StringPool.EQUAL));
			}
		}
	}

	private JSONObject _createCryptoUsageJSONObject() {
		return JSONUtil.put(
			"algorithm", "SHA-256"
		).put(
			"classNames", JSONUtil.putAll(RandomTestUtil.randomString())
		).put(
			"cryptoFunctions", JSONUtil.putAll("digest")
		).put(
			"cryptoPrimitive", "hash"
		).put(
			"featureName", RandomTestUtil.randomString()
		).put(
			"jcaClassName", "MessageDigest"
		);
	}

	private String _read(String name) throws Exception {
		try (InputStream inputStream =
				CryptoUsageDataUtil.class.getResourceAsStream(name)) {

			return StringUtil.read(inputStream);
		}
	}

	private void _testGetCryptoUsagesWithInvalidData(
			String expectedMessage, JSONObject jsonObject)
		throws Exception {

		try {
			CryptoUsageDataUtil.getCryptoUsages(
				JSONUtil.put("cryptoUsages", JSONUtil.putAll(jsonObject)));

			Assert.fail();
		}
		catch (IllegalArgumentException illegalArgumentException) {
			Assert.assertEquals(
				expectedMessage, illegalArgumentException.getMessage());
		}
	}

}