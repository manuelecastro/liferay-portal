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
import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

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
		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(false);

		cryptoCallScanner.scan(_getJarFile(DigesterUtil.class));
		cryptoCallScanner.scan(_getJarFile(JSONFactoryImpl.class));

		Set<String> problems = new TreeSet<>();

		List<CryptoUsage> cryptoUsages =
			CryptoUsageResolverUtil.getCryptoUsages(
				cryptoCallScanner.getCryptoCalls(),
				CryptoUsageDataUtil.getCryptoUsages(
					JSONFactoryUtil.createJSONObject(
						_read("dependencies/crypto-usages.json"))),
				problems);

		for (String problem : problems) {
			Assert.assertFalse(
				problem, problem.startsWith("Unable to resolve"));
		}

		String configurationName = RandomTestUtil.randomString();
		Properties portalExtProperties = new Properties();

		String cbomJSON = CBOMBuilder.build(
			configurationName, cryptoUsages, portalExtProperties);

		Assert.assertEquals(
			cbomJSON,
			CBOMBuilder.build(
				configurationName, cryptoUsages, portalExtProperties));

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

		JSONArray lifecyclesJSONArray = metadataJSONObject.getJSONArray(
			"lifecycles");

		JSONObject lifecycleJSONObject = lifecyclesJSONArray.getJSONObject(0);

		Assert.assertEquals(
			"post-build", lifecycleJSONObject.getString("phase"));

		JSONArray componentsJSONArray = jsonObject.getJSONArray("components");

		List<String> bomRefs = new ArrayList<>();
		Set<String> moduleBOMRefs = new TreeSet<>();

		for (int i = 0; i < componentsJSONArray.length(); i++) {
			componentJSONObject = componentsJSONArray.getJSONObject(i);

			String bomRef = componentJSONObject.getString("bom-ref");

			bomRefs.add(bomRef);

			if (!bomRef.startsWith("module:")) {
				continue;
			}

			moduleBOMRefs.add(bomRef);
		}

		List<String> sortedBOMRefs = new ArrayList<>(bomRefs);

		sortedBOMRefs.sort(null);

		Assert.assertEquals(sortedBOMRefs, bomRefs);

		Assert.assertTrue(
			bomRefs.toString(), bomRefs.contains("crypto:MessageDigest:MD5"));
		Assert.assertTrue(
			bomRefs.toString(),
			bomRefs.contains("crypto:MessageDigest:auth.mac.algorithm"));
		Assert.assertTrue(bomRefs.toString(), bomRefs.contains("provider:SUN"));
		Assert.assertTrue(
			bomRefs.toString(), bomRefs.contains("setting:auth.mac.algorithm"));
		Assert.assertFalse(moduleBOMRefs.isEmpty());

		JSONArray dependenciesJSONArray = jsonObject.getJSONArray(
			"dependencies");

		List<String> refs = new ArrayList<>();
		Set<String> moduleDependsOn = new TreeSet<>();

		for (int i = 0; i < dependenciesJSONArray.length(); i++) {
			JSONObject dependencyJSONObject =
				dependenciesJSONArray.getJSONObject(i);

			String ref = dependencyJSONObject.getString("ref");

			refs.add(ref);

			List<String> dependsOn = JSONUtil.toStringList(
				dependencyJSONObject.getJSONArray("dependsOn"));

			Assert.assertTrue(
				dependsOn.toString(), bomRefs.containsAll(dependsOn));

			if (moduleBOMRefs.contains(ref)) {
				moduleDependsOn.addAll(dependsOn);
			}

			List<String> provides = JSONUtil.toStringList(
				dependencyJSONObject.getJSONArray("provides"));

			Assert.assertTrue(
				provides.toString(), bomRefs.containsAll(provides));
		}

		Assert.assertEquals(bomRefs, refs);

		for (String bomRef : bomRefs) {
			if (!bomRef.startsWith("crypto:")) {
				continue;
			}

			Assert.assertTrue(bomRef, moduleDependsOn.contains(bomRef));
		}

		portalExtProperties.setProperty("fips.enabled", "true");

		try {
			CBOMBuilder.build(
				configurationName, cryptoUsages, portalExtProperties);

			Assert.fail();
		}
		catch (UnsupportedOperationException unsupportedOperationException) {
			Assert.assertEquals(
				"Unable to build the CBOM of a FIPS mode configuration",
				unsupportedOperationException.getMessage());
		}
	}

	private File _getJarFile(Class<?> clazz) throws Exception {
		ProtectionDomain protectionDomain = clazz.getProtectionDomain();

		CodeSource codeSource = protectionDomain.getCodeSource();

		URL url = codeSource.getLocation();

		return new File(url.toURI());
	}

	private String _read(String name) throws Exception {
		try (InputStream inputStream = CBOMBuilder.class.getResourceAsStream(
				name)) {

			return StringUtil.read(inputStream);
		}
	}

}