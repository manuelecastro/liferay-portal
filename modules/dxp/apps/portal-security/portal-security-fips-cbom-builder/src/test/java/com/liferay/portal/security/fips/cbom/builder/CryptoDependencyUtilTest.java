/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;

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
public class CryptoDependencyUtilTest {

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
	public void testToComponents() throws Exception {
		Bom bom = new Bom();

		bom.setComponents(CryptoDependencyUtil.toComponents(_cryptoUsages));

		BomJsonGenerator bomJsonGenerator = BomGeneratorFactory.createJson(
			Version.VERSION_16, bom);

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			bomJsonGenerator.toJsonString());

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				_toLibraryJSONObject(
					"module:Liferay Test Three", "Liferay Test Three"),
				_toLibraryJSONObject(
					"module:com.liferay.test.one", "Liferay Test One"),
				_toLibraryJSONObject(
					"module:com.liferay.test.two", "Liferay Test Two"),
				_toLibraryJSONObject("provider:SUN", "SUN"),
				_toDataJSONObject(
					"setting:keystore.type", "java.security", "keystore.type"),
				_toDataJSONObject(
					"setting:test.algorithm", "portal.properties",
					"test.algorithm")
			).toString(),
			String.valueOf(jsonObject.getJSONArray("components")),
			JSONCompareMode.STRICT);
	}

	@Test
	public void testToDependencies() throws Exception {
		Assert.assertEquals(
			Arrays.asList(
				"crypto:Cipher:test.algorithm [setting:test.algorithm]",
				"crypto:KeyStore:keystore.type [setting:keystore.type]",
				"crypto:MessageDigest:MD5 []",
				"crypto:MessageDigest:SHA-256 []",
				"crypto:SecretKeySpec:AES []",
				"module:Liferay Test Three [crypto:MessageDigest:MD5]",
				"module:com.liferay.test.one [crypto:MessageDigest:SHA-256, " +
					"crypto:SecretKeySpec:AES]",
				"module:com.liferay.test.two [crypto:Cipher:test.algorithm, " +
					"crypto:KeyStore:keystore.type]",
				"provider:SUN []", "setting:keystore.type []",
				"setting:test.algorithm []"),
			TransformUtil.transform(
				CryptoDependencyUtil.toDependencies(_cryptoUsages),
				dependency -> {
					List<String> refs = TransformUtil.transform(
						dependency.getDependencies(), Dependency::getRef);

					return dependency.getRef() + " " + refs;
				}));
	}

	@Test
	public void testToProvides() throws Exception {
		Assert.assertEquals(
			HashMapBuilder.put(
				"provider:SUN",
				new TreeSet<>(
					Arrays.asList(
						"crypto:MessageDigest:MD5",
						"crypto:MessageDigest:SHA-256"))
			).build(),
			CryptoDependencyUtil.toProvides(_cryptoUsages));
	}

	private static CryptoUsage _createCryptoUsage(
		String algorithm, String bundleSymbolicName, String className,
		List<String> configuredBy, String featureName, String jcaClassName) {

		CryptoUsage cryptoUsage = new CryptoUsage(
			bundleSymbolicName, Collections.singletonList(className),
			Collections.singletonList(CryptoFunction.OTHER), featureName,
			jcaClassName, Primitive.UNKNOWN);

		cryptoUsage.setAlgorithm(algorithm);
		cryptoUsage.setConfiguredBy(configuredBy);

		return cryptoUsage;
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

	private static final List<CryptoUsage> _cryptoUsages = Arrays.asList(
		_createCryptoUsage(
			"SHA-256", "com.liferay.test.one", "com.liferay.test.Class1",
			Collections.emptyList(), "Liferay Test One", "MessageDigest"),
		_createCryptoUsage(
			"AES", "com.liferay.test.one", "com.liferay.test.Class1",
			Collections.emptyList(), "Liferay Test One", "SecretKeySpec"),
		_createCryptoUsage(
			null, "com.liferay.test.two", "com.liferay.test.Class2",
			Collections.singletonList("test.algorithm"), "Liferay Test Two",
			"Cipher"),
		_createCryptoUsage(
			null, "com.liferay.test.two", "com.liferay.test.Class3",
			Collections.singletonList("keystore.type"), "Liferay Test Two",
			"KeyStore"),
		_createCryptoUsage(
			"MD5", null, "com.liferay.test.Class4", Collections.emptyList(),
			"Liferay Test Three", "MessageDigest"));

}