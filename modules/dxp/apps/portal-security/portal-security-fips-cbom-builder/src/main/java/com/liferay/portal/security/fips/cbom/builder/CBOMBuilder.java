/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.fips.FIPSModeValidator;
import com.liferay.portal.kernel.security.xml.SecureXMLFactoryProviderUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropertiesUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.security.xml.SecureXMLFactoryProviderImpl;

import java.io.File;
import java.io.FileInputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.security.Provider;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Property;

/**
 * @author Manuele Castro
 */
public class CBOMBuilder {

	public static String build(
			String configurationName, CryptoAssessment cryptoAssessment,
			List<CryptoCall> cryptoCalls, Map<String, String> properties,
			int version)
		throws Exception {

		List<Component> components = new ArrayList<>(
			CryptoAssetComponentUtil.toComponents(
				cryptoAssessment, cryptoCalls));

		for (Component component :
				CryptoDependencyUtil.toComponents(cryptoCalls)) {

			if (cryptoAssessment.isFIPSModuleProvider(component.getName())) {
				_setModule(component, cryptoAssessment);
			}

			components.add(component);
		}

		components.sort(Comparator.comparing(Component::getBomRef));

		return CycloneDXCBOMWriter.write(
			components, configurationName,
			CryptoDependencyUtil.toDependencies(cryptoCalls),
			ReleaseInfo.getName(), ReleaseInfo.getVersionDisplayName(),
			properties, CryptoDependencyUtil.toProvides(cryptoCalls), version);
	}

	public static void main(String[] args) throws Exception {
		File configurationDir = new File(args[0]);

		Properties portalExtProperties = _loadProperties(
			new File(configurationDir, "portal-ext.properties"));

		boolean fipsEnabled = GetterUtil.getBoolean(
			portalExtProperties.getProperty("fips.enabled"));

		if (fipsEnabled) {
			for (String key : portalExtProperties.stringPropertyNames()) {
				PropsUtil.set(key, portalExtProperties.getProperty(key));
			}

			try {
				FIPSModeValidator.validate();
			}
			catch (SecurityException securityException) {
				String message = StringBundler.concat(
					"Unable to build the CBOM of ", configurationDir.getName(),
					" because its configuration fails FIPS mode's startup ",
					"checks: ", securityException.getMessage());

				if (_isRelease()) {
					throw new IllegalStateException(message, securityException);
				}

				System.out.println("WARNING: " + message);

				return;
			}
		}

		SecureXMLFactoryProviderUtil secureXMLFactoryProviderUtil =
			new SecureXMLFactoryProviderUtil();

		secureXMLFactoryProviderUtil.setSecureXMLFactoryProvider(
			new SecureXMLFactoryProviderImpl());

		CryptoCallScanner cryptoCallScanner = new CryptoCallScanner(
			fipsEnabled);

		for (File file : _getFiles(new File(args[3]), new File(args[2]))) {
			cryptoCallScanner.scan(file);
		}

		List<CryptoCall> cryptoCalls = cryptoCallScanner.getCryptoCalls();

		CryptoAssessment cryptoAssessment = CryptoAssessment.load(null, null);
		Map<String, String> properties = new TreeMap<>();

		if (fipsEnabled) {
			cryptoCalls = FIPSModeLimitUtil.limit(
				cryptoCalls, cryptoCallScanner.getLimitedSettings());

			Provider provider = FIPSModeValidator.fetchProvider();

			String providerVersion = CryptoProviderUtil.fetchProviderVersion(
				provider.getName());

			cryptoAssessment = CryptoAssessment.load(
				provider.getName(), providerVersion);

			if (!cryptoAssessment.hasFIPSModule()) {
				_reportFailure(
					StringBundler.concat(
						"The CBOM reference data has no FIPS module for ",
						provider.getName(), StringPool.SPACE, providerVersion));
			}

			String message = cryptoAssessment.fetchInactiveCertificateMessage(
				LocalDate.now());

			if (message != null) {
				_reportFailure(message);
			}

			properties.put(
				"liferay:cbom:fips-provider",
				provider.getName() + StringPool.SPACE + providerVersion);
		}

		Set<String> messages = new TreeSet<>();

		for (CryptoCall cryptoCall : cryptoCalls) {
			if (cryptoCall.isResolved()) {
				continue;
			}

			messages.add(
				StringBundler.concat(
					"The scan cannot read the algorithm of the ",
					cryptoCall.getJCAClassName(), " call in ",
					cryptoCall.getClassName(), ", which comes from ",
					cryptoCall.getUnresolvedReason()));
		}

		for (String message : messages) {
			System.out.println(message);
		}

		cryptoCalls = new ArrayList<>(cryptoCalls);

		cryptoCalls.addAll(
			CryptoEnvironmentUtil.getCryptoCalls(
				cryptoAssessment, cryptoCalls, fipsEnabled));

		String cbomJSON = build(
			configurationDir.getName(), cryptoAssessment, cryptoCalls,
			properties,
			GetterUtil.getInteger(System.getProperty("cbom.version"), 1));

		List<String> schemaMessages = CycloneDXCBOMWriter.validate(cbomJSON);

		if (!schemaMessages.isEmpty()) {
			_reportFailure(
				StringBundler.concat(
					"The CBOM of ", configurationDir.getName(),
					" fails the CycloneDX schema: ",
					StringUtil.merge(schemaMessages, "; ")));
		}

		File file = new File(args[1]);

		File parentDir = file.getParentFile();

		parentDir.mkdirs();

		Files.write(file.toPath(), cbomJSON.getBytes(StandardCharsets.UTF_8));

		System.out.println("Wrote " + file.getAbsolutePath());
	}

	private static void _addFiles(
		File dir, List<File> excludedDirs, List<File> files) {

		File[] childFiles = dir.listFiles();

		if (childFiles == null) {
			return;
		}

		for (File childFile : childFiles) {
			String fileName = childFile.getName();

			if (childFile.isDirectory()) {
				if (!excludedDirs.contains(childFile)) {
					_addFiles(childFile, excludedDirs, files);
				}
			}
			else if (fileName.endsWith(".jar") || fileName.endsWith(".lpkg")) {
				files.add(childFile);
			}
		}
	}

	private static List<File> _getFiles(
		File appServerPortalDir, File liferayHomeDir) {

		File osgiDir = new File(liferayHomeDir, "osgi");

		if (!osgiDir.isDirectory()) {
			throw new IllegalArgumentException(
				"Directory \"" + osgiDir + "\" does not exist");
		}

		File libDir = new File(appServerPortalDir, "WEB-INF/lib");

		if (!libDir.isDirectory()) {
			throw new IllegalArgumentException(
				"Directory \"" + libDir + "\" does not exist");
		}

		List<File> files = new ArrayList<>();

		_addFiles(
			osgiDir,
			Arrays.asList(
				new File(osgiDir, "state"), new File(osgiDir, "test")),
			files);

		_addFiles(libDir, Collections.emptyList(), files);
		_addFiles(
			new File(appServerPortalDir, "WEB-INF/shielded-container-lib"),
			Collections.emptyList(), files);

		files.sort(null);

		return files;
	}

	private static boolean _isRelease() {
		return GetterUtil.getBoolean(System.getProperty("cbom.release"));
	}

	private static Properties _loadProperties(File file) throws Exception {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			return PropertiesUtil.load(fileInputStream, StringPool.UTF8);
		}
	}

	private static void _reportFailure(String message) {
		if (_isRelease()) {
			throw new IllegalStateException(message);
		}

		System.out.println("WARNING: " + message);
	}

	private static void _setModule(
		Component component, CryptoAssessment cryptoAssessment) {

		Map<String, String> properties = new TreeMap<>(
			cryptoAssessment.getModuleProperties());

		List<String> vendorAffirmedMethods =
			cryptoAssessment.getVendorAffirmedMethods();

		if (!vendorAffirmedMethods.isEmpty()) {
			properties.put(
				"liferay:cbom:vendor-affirmed-methods",
				StringUtil.merge(vendorAffirmedMethods, "; "));
		}

		component.setProperties(
			TransformUtil.transform(
				properties.entrySet(),
				entry -> new Property(entry.getKey(), entry.getValue())));

		ExternalReference externalReference = new ExternalReference();

		externalReference.setType(ExternalReference.Type.CERTIFICATION_REPORT);
		externalReference.setUrl(cryptoAssessment.fetchCertificateURL());

		component.setExternalReferences(
			Collections.singletonList(externalReference));
	}

}