/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.cyclonedx.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.parsers.JsonParser;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CBOMBuilderTest {

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
	public void testBuild() throws Exception {
		String configurationName = RandomTestUtil.randomString();
		List<CryptoUsage> cryptoUsages = CryptoUsageDataUtil.getCryptoUsages(
			JSONFactoryUtil.createJSONObject(
				_read("dependencies/crypto-usages.json")));
		Properties portalExtProperties = new Properties();

		Properties portalProperties = new Properties();

		ClassLoader classLoader = CBOMBuilder.class.getClassLoader();

		try (InputStream inputStream = classLoader.getResourceAsStream(
				"portal.properties")) {

			portalProperties.load(inputStream);
		}

		String cbomJSON = CBOMBuilder.build(
			configurationName, cryptoUsages, portalExtProperties,
			portalProperties);

		Assert.assertEquals(
			cbomJSON,
			CBOMBuilder.build(
				configurationName, cryptoUsages, portalExtProperties,
				portalProperties));

		JsonParser jsonParser = new JsonParser();

		List<ParseException> parseExceptions = jsonParser.validate(
			cbomJSON.getBytes(StandardCharsets.UTF_8), Version.VERSION_16);

		Assert.assertEquals(
			parseExceptions.toString(), 0, parseExceptions.size());

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(cbomJSON);

		JSONObject metadataJSONObject = jsonObject.getJSONObject("metadata");

		JSONObject componentJSONObject = metadataJSONObject.getJSONObject(
			"component");

		Assert.assertEquals(
			ReleaseInfo.getName(), componentJSONObject.getString("name"));
		Assert.assertEquals(
			ReleaseInfo.getVersionDisplayName(),
			componentJSONObject.getString("version"));

		Assert.assertFalse(metadataJSONObject.has("timestamp"));

		JSONArray componentsJSONArray = jsonObject.getJSONArray("components");

		List<String> bomRefs = new ArrayList<>();

		for (int i = 0; i < componentsJSONArray.length(); i++) {
			componentJSONObject = componentsJSONArray.getJSONObject(i);

			bomRefs.add(componentJSONObject.getString("bom-ref"));
		}

		List<String> sortedBOMRefs = new ArrayList<>(bomRefs);

		sortedBOMRefs.sort(null);

		Assert.assertEquals(sortedBOMRefs, bomRefs);

		Assert.assertTrue(
			bomRefs.toString(), bomRefs.contains("crypto:MessageDigest:MD5"));
		Assert.assertTrue(
			bomRefs.toString(),
			bomRefs.contains("crypto:company.encryption.algorithm"));

		portalExtProperties.setProperty("fips.enabled", "true");

		try {
			CBOMBuilder.build(
				configurationName, cryptoUsages, portalExtProperties,
				portalProperties);

			Assert.fail();
		}
		catch (UnsupportedOperationException unsupportedOperationException) {
			Assert.assertEquals(
				"Unable to build the CBOM of a FIPS mode configuration",
				unsupportedOperationException.getMessage());
		}
	}

	private String _read(String name) throws Exception {
		try (InputStream inputStream = CBOMBuilder.class.getResourceAsStream(
				name)) {

			return StringUtil.read(inputStream);
		}
	}

}