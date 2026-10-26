/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.List;
import java.util.Properties;

/**
 * Builds the CBOM of one reference configuration. The build runs it in a JVM
 * that is set up as that configuration, so the providers it reports are the
 * ones that the configuration's JVM resolves.
 *
 * @author Manuele Castro
 */
public class CBOMBuilder {

	public static String build(
			String configurationName, List<CryptoUsage> cryptoUsages,
			Properties portalExtProperties, Properties portalProperties)
		throws Exception {

		if (GetterUtil.getBoolean(
				portalExtProperties.getProperty("fips.enabled"))) {

			throw new UnsupportedOperationException(
				"Unable to build the CBOM of a FIPS mode configuration");
		}

		return CycloneDXCBOMWriter.write(
			CryptoAssetComponentUtil.toJSONArray(
				cryptoUsages, portalProperties),
			configurationName, ReleaseInfo.getName(),
			ReleaseInfo.getVersionDisplayName());
	}

	public static void main(String[] args) throws Exception {
		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		jsonFactoryUtil.setJSONFactory(new JSONFactoryImpl());

		File configurationDir = new File(args[0]);

		ClassLoader classLoader = CBOMBuilder.class.getClassLoader();

		String cbomJSON = build(
			configurationDir.getName(),
			CryptoUsageDataUtil.getCryptoUsages(
				JSONFactoryUtil.createJSONObject(
					_read("dependencies/crypto-usages.json"))),
			_loadProperties(
				new FileInputStream(
					new File(configurationDir, "portal-ext.properties"))),
			_loadProperties(
				classLoader.getResourceAsStream("portal.properties")));

		File file = new File(args[1]);

		File parentDir = file.getParentFile();

		parentDir.mkdirs();

		Files.write(file.toPath(), cbomJSON.getBytes(StandardCharsets.UTF_8));

		System.out.println("Wrote " + file.getAbsolutePath());
	}

	private static Properties _loadProperties(InputStream inputStream)
		throws Exception {

		Properties properties = new Properties();

		try (InputStream propertiesInputStream = inputStream) {
			properties.load(propertiesInputStream);
		}

		return properties;
	}

	private static String _read(String name) throws Exception {
		try (InputStream inputStream = CBOMBuilder.class.getResourceAsStream(
				name)) {

			return StringUtil.read(inputStream);
		}
	}

}