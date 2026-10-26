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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.cyclonedx.model.Component;

/**
 * Builds the CBOM of one reference configuration from the bytecode of the JARs
 * the bundle ships, and from the usage data for the calls the scan cannot
 * resolve. The CBOM holds the crypto assets, the modules that use them, the
 * providers that answer them and the settings that choose them, and the
 * dependency graph between them. The build runs it in a JVM that is set up as
 * that configuration, so the providers it reports are the ones that the
 * configuration's JVM resolves. A call the usage data should describe, or a
 * description no call needs, is a warning, and fails a release build.
 *
 * @author Manuele Castro
 */
public class CBOMBuilder {

	public static String build(
			String configurationName, List<CryptoUsage> cryptoUsages,
			Properties portalExtProperties)
		throws Exception {

		if (GetterUtil.getBoolean(
				portalExtProperties.getProperty("fips.enabled"))) {

			throw new UnsupportedOperationException(
				"Unable to build the CBOM of a FIPS mode configuration");
		}

		List<Component> components = new ArrayList<>(
			CryptoAssetComponentUtil.toComponents(cryptoUsages));

		components.addAll(CryptoDependencyUtil.toComponents(cryptoUsages));

		components.sort(Comparator.comparing(Component::getBomRef));

		return CycloneDXCBOMWriter.write(
			components, configurationName,
			CryptoDependencyUtil.toDependencies(cryptoUsages),
			ReleaseInfo.getName(), ReleaseInfo.getVersionDisplayName(),
			CryptoDependencyUtil.toProvides(cryptoUsages));
	}

	public static void main(String[] args) throws Exception {
		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		jsonFactoryUtil.setJSONFactory(new JSONFactoryImpl());

		File configurationDir = new File(args[0]);

		Properties portalExtProperties = _loadProperties(
			new FileInputStream(
				new File(configurationDir, "portal-ext.properties")));

		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(
			GetterUtil.getBoolean(
				portalExtProperties.getProperty("fips.enabled")));

		for (File file : _getFiles(new File(args[3]), new File(args[2]))) {
			cryptoCallScanner.scan(file);
		}

		Set<String> problems = new TreeSet<>();

		List<CryptoUsage> cryptoUsages =
			CryptoUsageResolverUtil.getCryptoUsages(
				cryptoCallScanner.getCryptoCalls(),
				CryptoUsageDataUtil.getCryptoUsages(
					JSONFactoryUtil.createJSONObject(
						_read("dependencies/crypto-usages.json"))),
				problems);

		for (String problem : problems) {
			System.out.println("Warning: " + problem);
		}

		if (!problems.isEmpty() &&
			GetterUtil.getBoolean(System.getProperty("cbom.release"))) {

			throw new IllegalStateException(
				"Unable to build a release CBOM with " + problems.size() +
					" problems");
		}

		String cbomJSON = build(
			configurationDir.getName(), cryptoUsages, portalExtProperties);

		File file = new File(args[1]);

		File parentDir = file.getParentFile();

		parentDir.mkdirs();

		Files.write(file.toPath(), cbomJSON.getBytes(StandardCharsets.UTF_8));

		System.out.println("Wrote " + file.getAbsolutePath());
	}

	private static void _addFiles(
		File dir, File excludedDir, List<File> files) {

		File[] childFiles = dir.listFiles();

		if (childFiles == null) {
			return;
		}

		for (File childFile : childFiles) {
			String fileName = childFile.getName();

			if (childFile.isDirectory()) {
				if (!childFile.equals(excludedDir)) {
					_addFiles(childFile, excludedDir, files);
				}
			}
			else if (fileName.endsWith(".jar") || fileName.endsWith(".lpkg")) {
				files.add(childFile);
			}
		}
	}

	private static List<File> _getFiles(
		File appServerPortalDir, File liferayHomeDir) {

		List<File> files = new ArrayList<>();

		File osgiDir = new File(liferayHomeDir, "osgi");

		_addFiles(osgiDir, new File(osgiDir, "test"), files);

		_addFiles(new File(appServerPortalDir, "WEB-INF/lib"), null, files);
		_addFiles(
			new File(appServerPortalDir, "WEB-INF/shielded-container-lib"),
			null, files);

		files.sort(null);

		return files;
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