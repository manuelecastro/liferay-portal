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
import com.liferay.portal.kernel.security.xml.SecureXMLFactoryProviderUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.DigesterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.security.xml.SecureXMLFactoryProviderImpl;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.io.File;

import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.junit.rules.TemporaryFolder;

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

		SecureXMLFactoryProviderUtil secureXMLFactoryProviderUtil =
			new SecureXMLFactoryProviderUtil();

		secureXMLFactoryProviderUtil.setSecureXMLFactoryProvider(
			new SecureXMLFactoryProviderImpl());
	}

	@Test
	public void testBuild() throws Exception {
		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(false);

		cryptoCallScanner.scan(_getJarFile(DigesterUtil.class));
		cryptoCallScanner.scan(_getJarFile(JSONFactoryImpl.class));

		List<CryptoCall> cryptoCalls = cryptoCallScanner.getCryptoCalls();

		String configurationName = RandomTestUtil.randomString();

		CryptoAssessment cryptoAssessment = CryptoAssessment.load(null, null);

		String cbomJSON = CBOMBuilder.build(
			configurationName, cryptoAssessment, cryptoCalls,
			Collections.emptyMap());

		Assert.assertEquals(
			cbomJSON,
			CBOMBuilder.build(
				configurationName, cryptoAssessment, cryptoCalls,
				Collections.emptyMap()));

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
		Map<String, String> evidences = new HashMap<>();
		Set<String> moduleBOMRefs = new TreeSet<>();
		Map<String, String> properties = new HashMap<>();

		for (int i = 0; i < componentsJSONArray.length(); i++) {
			componentJSONObject = componentsJSONArray.getJSONObject(i);

			String bomRef = componentJSONObject.getString("bom-ref");

			bomRefs.add(bomRef);
			evidences.put(bomRef, componentJSONObject.getString("evidence"));
			properties.put(bomRef, componentJSONObject.getString("properties"));

			if (!bomRef.startsWith("module:")) {
				continue;
			}

			moduleBOMRefs.add(bomRef);
		}

		List<String> sortedBOMRefs = new ArrayList<>(bomRefs);

		sortedBOMRefs.sort(null);

		Assert.assertEquals(sortedBOMRefs, bomRefs);

		Assert.assertTrue(bomRefs.contains("crypto:MessageDigest:MD5"));
		Assert.assertTrue(
			bomRefs.contains("crypto:MessageDigest:auth.mac.algorithm"));
		Assert.assertTrue(bomRefs.contains("provider:SUN"));
		Assert.assertTrue(bomRefs.contains("setting:auth.mac.algorithm"));
		Assert.assertFalse(moduleBOMRefs.isEmpty());

		String evidence = evidences.get("crypto:MessageDigest:SHA-1");

		String sha256Properties = properties.get(
			"crypto:MessageDigest:SHA-256");

		Assert.assertTrue(sha256Properties.contains("PROVIDER_NOT_VALIDATED"));

		Assert.assertTrue(
			evidence.contains(
				"com.liferay.portal.cache.key.MessageDigestCacheKeyGenerator"));

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

			Assert.assertTrue(bomRefs.containsAll(dependsOn));

			if (moduleBOMRefs.contains(ref)) {
				moduleDependsOn.addAll(dependsOn);
			}

			List<String> provides = JSONUtil.toStringList(
				dependencyJSONObject.getJSONArray("provides"));

			Assert.assertTrue(bomRefs.containsAll(provides));
		}

		Assert.assertEquals(bomRefs, refs);

		for (String bomRef : bomRefs) {
			if (!bomRef.startsWith("crypto:")) {
				continue;
			}

			Assert.assertTrue(moduleDependsOn.contains(bomRef));
		}
	}

	@Test
	public void testMain() throws Exception {
		File configurationDir = temporaryFolder.newFolder();

		File portalExtPropertiesFile = new File(
			configurationDir, "portal-ext.properties");

		Files.write(
			portalExtPropertiesFile.toPath(),
			"fips.enabled=true".getBytes(StandardCharsets.UTF_8));

		File file = new File(temporaryFolder.getRoot(), "bcfips.cdx.json");

		String[] args = {configurationDir.getPath(), file.getPath()};

		try {
			CBOMBuilder.main(args);

			Assert.assertFalse(file.exists());

			System.setProperty("cbom.release", "true");

			CBOMBuilder.main(args);

			Assert.fail();
		}
		catch (IllegalStateException illegalStateException) {
			String message = illegalStateException.getMessage();

			Assert.assertTrue(
				message.contains(
					"The first security provider must be an allowed FIPS " +
						"provider"));
		}
		finally {
			System.clearProperty("cbom.release");

			PropsUtil.set("fips.enabled", "false");
		}
	}

	@Test
	public void testMainWhenOSGiDirIsMissing() throws Exception {
		File configurationDir = temporaryFolder.newFolder();

		File portalExtPropertiesFile = new File(
			configurationDir, "portal-ext.properties");

		Files.write(
			portalExtPropertiesFile.toPath(),
			"fips.enabled=false".getBytes(StandardCharsets.UTF_8));

		File file = new File(temporaryFolder.getRoot(), "fips-off.cdx.json");
		File liferayHomeDir = temporaryFolder.newFolder();

		try {
			CBOMBuilder.main(
				new String[] {
					configurationDir.getPath(), file.getPath(),
					liferayHomeDir.getPath(),
					temporaryFolder.newFolder(
					).getPath()
				});

			Assert.fail();
		}
		catch (IllegalArgumentException illegalArgumentException) {
			Assert.assertEquals(
				StringBundler.concat(
					"Directory \"", new File(liferayHomeDir, "osgi"),
					"\" does not exist"),
				illegalArgumentException.getMessage());
		}

		Assert.assertFalse(file.exists());
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File _getJarFile(Class<?> clazz) throws Exception {
		ProtectionDomain protectionDomain = clazz.getProtectionDomain();

		CodeSource codeSource = protectionDomain.getCodeSource();

		URL url = codeSource.getLocation();

		return new File(url.toURI());
	}

}