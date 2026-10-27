/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Manuele Castro
 */
public class CycloneDXCBOMWriterTest {

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
	public void testWrite() throws Exception {
		String configurationName = RandomTestUtil.randomString();
		String productName = RandomTestUtil.randomString();
		String productVersion = RandomTestUtil.randomString();

		String json = CycloneDXCBOMWriter.write(
			configurationName, _cryptoCalls, productName, productVersion);

		Assert.assertEquals(
			json,
			CycloneDXCBOMWriter.write(
				configurationName, _cryptoCalls, productName, productVersion));

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(json);

		Assert.assertEquals("CycloneDX", jsonObject.getString("bomFormat"));

		JSONArray componentsJSONArray = jsonObject.getJSONArray("components");

		Assert.assertEquals(
			Arrays.asList(
				"crypto:Cipher:test.algorithm", "crypto:KeyStore:keystore.type",
				"crypto:MessageDigest:MD5", "crypto:MessageDigest:SHA-256",
				"crypto:MessageDigest:com.liferay.test.Class5",
				"crypto:SecretKeySpec:AES", "module:com.liferay.test.one",
				"module:com.liferay.test.three", "module:com.liferay.test.two",
				"provider:SUN", "setting:keystore.type",
				"setting:test.algorithm"),
			JSONUtil.toList(
				componentsJSONArray,
				componentJSONObject -> componentJSONObject.getString(
					"bom-ref")));

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				_toLibraryJSONObject(
					"module:com.liferay.test.one", "Liferay Test One"),
				_toLibraryJSONObject(
					"module:com.liferay.test.three", "Liferay Test Three"),
				_toLibraryJSONObject(
					"module:com.liferay.test.two", "Liferay Test Two"),
				_toLibraryJSONObject("provider:SUN", "SUN"),
				_toDataJSONObject(
					"setting:keystore.type", "java.security", "keystore.type"),
				_toDataJSONObject(
					"setting:test.algorithm", "portal.properties",
					"test.algorithm")
			).toString(),
			String.valueOf(
				JSONUtil.toJSONArray(
					JSONUtil.toList(
						componentsJSONArray,
						componentJSONObject -> {
							String bomRef = componentJSONObject.getString(
								"bom-ref");

							if (bomRef.startsWith("crypto:")) {
								return null;
							}

							return componentJSONObject;
						}),
					componentJSONObject -> componentJSONObject)),
			JSONCompareMode.STRICT);

		Assert.assertEquals(
			Arrays.asList(
				"crypto:Cipher:test.algorithm [setting:test.algorithm] []",
				"crypto:KeyStore:keystore.type [setting:keystore.type] []",
				"crypto:MessageDigest:MD5 [] []",
				"crypto:MessageDigest:SHA-256 [] []",
				"crypto:MessageDigest:com.liferay.test.Class5 [] []",
				"crypto:SecretKeySpec:AES [] []",
				"module:com.liferay.test.one [crypto:MessageDigest:SHA-256, " +
					"crypto:MessageDigest:com.liferay.test.Class5, " +
						"crypto:SecretKeySpec:AES] []",
				"module:com.liferay.test.three [crypto:MessageDigest:MD5] []",
				"module:com.liferay.test.two [crypto:Cipher:test.algorithm, " +
					"crypto:KeyStore:keystore.type] []",
				"provider:SUN [] [crypto:MessageDigest:MD5, " +
					"crypto:MessageDigest:SHA-256]",
				"setting:keystore.type [] []", "setting:test.algorithm [] []"),
			JSONUtil.toList(
				jsonObject.getJSONArray("dependencies"),
				dependencyJSONObject -> StringBundler.concat(
					dependencyJSONObject.getString("ref"), " ",
					JSONUtil.toStringList(
						dependencyJSONObject.getJSONArray("dependsOn")),
					" ",
					JSONUtil.toStringList(
						dependencyJSONObject.getJSONArray("provides")))));

		JSONAssert.assertEquals(
			JSONUtil.put(
				"component",
				JSONUtil.put(
					"name", productName
				).put(
					"type", "application"
				).put(
					"version", productVersion
				)
			).put(
				"lifecycles",
				JSONUtil.putAll(JSONUtil.put("phase", "post-build"))
			).put(
				"properties",
				JSONUtil.putAll(
					JSONUtil.put(
						"name", "liferay:cbom:configuration"
					).put(
						"value", configurationName
					),
					JSONUtil.put(
						"name", "liferay:cbom:schema-version"
					).put(
						"value", "1.0"
					))
			).toString(),
			String.valueOf(jsonObject.getJSONObject("metadata")),
			JSONCompareMode.STRICT);

		String serialNumber = jsonObject.getString("serialNumber");

		Assert.assertTrue(
			serialNumber.matches(
				"urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-" +
					"[89ab][0-9a-f]{3}-[0-9a-f]{12}"));

		Assert.assertEquals("1.6", jsonObject.getString("specVersion"));
		Assert.assertEquals(1, jsonObject.getInt("version"));

		Assert.assertNotEquals(
			json,
			CycloneDXCBOMWriter.write(
				RandomTestUtil.randomString(), _cryptoCalls, productName,
				productVersion));
		Assert.assertNotEquals(
			json,
			CycloneDXCBOMWriter.write(
				configurationName, _cryptoCalls, productName,
				RandomTestUtil.randomString()));
	}

	private static CryptoCall _createCryptoCall(
		String algorithm, String bundleSymbolicName, String className,
		List<String> configuredBy, String featureName, String jcaClassName,
		String unresolvedReason) {

		CryptoCall cryptoCall = new CryptoCall(
			bundleSymbolicName, className, featureName, jcaClassName);

		cryptoCall.setAlgorithm(algorithm);
		cryptoCall.setConfiguredBy(configuredBy);
		cryptoCall.setUnresolvedReason(unresolvedReason);

		return cryptoCall;
	}

	private JSONObject _toDataJSONObject(
		String bomRef, String dataName, String name) {

		return JSONUtil.put(
			"bom-ref", bomRef
		).put(
			"data",
			JSONUtil.putAll(
				JSONUtil.put(
					"name", dataName
				).put(
					"type", "configuration"
				))
		).put(
			"name", name
		).put(
			"type", "data"
		);
	}

	private JSONObject _toLibraryJSONObject(String bomRef, String name) {
		return JSONUtil.put(
			"bom-ref", bomRef
		).put(
			"name", name
		).put(
			"type", "library"
		);
	}

	private static final List<CryptoCall> _cryptoCalls = Arrays.asList(
		_createCryptoCall(
			"SHA-256", "com.liferay.test.one", RandomTestUtil.randomString(),
			Collections.emptyList(), "Liferay Test One", "MessageDigest", null),
		_createCryptoCall(
			"AES", "com.liferay.test.one", RandomTestUtil.randomString(),
			Collections.emptyList(), "Liferay Test One", "SecretKeySpec", null),
		_createCryptoCall(
			null, "com.liferay.test.two", RandomTestUtil.randomString(),
			Collections.singletonList("test.algorithm"), "Liferay Test Two",
			"Cipher", null),
		_createCryptoCall(
			null, "com.liferay.test.two", RandomTestUtil.randomString(),
			Collections.singletonList("keystore.type"), "Liferay Test Two",
			"KeyStore", null),
		_createCryptoCall(
			"MD5", "com.liferay.test.three", RandomTestUtil.randomString(),
			Collections.emptyList(), "Liferay Test Three", "MessageDigest",
			null),
		_createCryptoCall(
			null, "com.liferay.test.one", "com.liferay.test.Class5",
			Collections.emptyList(), "Liferay Test One", "MessageDigest",
			RandomTestUtil.randomString()));

}