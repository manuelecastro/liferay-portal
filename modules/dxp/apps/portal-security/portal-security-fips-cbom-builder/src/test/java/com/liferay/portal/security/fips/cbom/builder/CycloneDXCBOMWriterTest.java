/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

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
		JSONArray componentsJSONArray = JSONUtil.putAll(
			JSONUtil.put("bom-ref", RandomTestUtil.randomString()));
		String configurationName = RandomTestUtil.randomString();
		String productName = RandomTestUtil.randomString();
		String productVersion = RandomTestUtil.randomString();

		String json = CycloneDXCBOMWriter.write(
			componentsJSONArray, configurationName, productName,
			productVersion);

		Assert.assertEquals(
			json,
			CycloneDXCBOMWriter.write(
				componentsJSONArray, configurationName, productName,
				productVersion));

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(json);

		String serialNumber = jsonObject.getString("serialNumber");

		Assert.assertTrue(
			serialNumber,
			serialNumber.matches(
				"urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-" +
					"[89ab][0-9a-f]{3}-[0-9a-f]{12}"));

		JSONAssert.assertEquals(
			JSONUtil.put(
				"$schema", "http://cyclonedx.org/schema/bom-1.6.schema.json"
			).put(
				"bomFormat", "CycloneDX"
			).put(
				"components", componentsJSONArray
			).put(
				"metadata",
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
				)
			).put(
				"serialNumber", serialNumber
			).put(
				"specVersion", "1.6"
			).put(
				"version", 1
			).toString(),
			json, JSONCompareMode.STRICT);

		Assert.assertNotEquals(
			json,
			CycloneDXCBOMWriter.write(
				componentsJSONArray, RandomTestUtil.randomString(), productName,
				productVersion));
		Assert.assertNotEquals(
			json,
			CycloneDXCBOMWriter.write(
				componentsJSONArray, configurationName, productName,
				RandomTestUtil.randomString()));
	}

}