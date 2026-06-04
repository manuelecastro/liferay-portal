/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.saml.internal.upgrade.v2_0_0;

import com.liferay.document.library.kernel.exception.NoSuchFileException;
import com.liferay.document.library.kernel.store.Store;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProviderUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.saml.runtime.certificate.CertificateTool;
import com.liferay.saml.runtime.configuration.SamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.security.KeyStore;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Rafael Praxedes
 */
public class SamlKeyStoreTypeUpgradeProcess extends UpgradeProcess {

	public SamlKeyStoreTypeUpgradeProcess(
		CertificateTool certificateTool,
		CompanyLocalService companyLocalService,
		ConfigurationAdmin configurationAdmin, Store store) {

		_certificateTool = certificateTool;
		_companyLocalService = companyLocalService;
		_configurationAdmin = configurationAdmin;
		_store = store;
	}

	@Override
	protected void doUpgrade() throws Exception {
		_upgradeConfiguration();
		_upgradeDLKeystores();
		_upgradeFileSystemKeystore();
	}

	private KeyStore _convertJKSToPKCS12(
			InputStream inputStream, char[] password)
		throws Exception {

		KeyStore jksKeyStore = KeyStore.getInstance("JKS");

		jksKeyStore.load(inputStream, password);

		KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

		pkcs12KeyStore.load(null, null);

		Enumeration<String> aliasesEnumeration = jksKeyStore.aliases();

		while (aliasesEnumeration.hasMoreElements()) {
			String alias = aliasesEnumeration.nextElement();

			if (jksKeyStore.isKeyEntry(alias)) {
				KeyStore.Entry entry = jksKeyStore.getEntry(
					alias, new KeyStore.PasswordProtection(password));

				pkcs12KeyStore.setEntry(
					alias, entry, new KeyStore.PasswordProtection(password));
			}
			else if (jksKeyStore.isCertificateEntry(alias)) {
				pkcs12KeyStore.setCertificateEntry(
					alias, jksKeyStore.getCertificate(alias));
			}
		}

		return pkcs12KeyStore;
	}

	private String _getKeystorePassword() {
		try {
			Configuration configuration = _configurationAdmin.getConfiguration(
				_SAML_CONFIGURATION_PID, StringPool.QUESTION);

			Dictionary<String, Object> properties =
				configuration.getProperties();

			if (properties != null) {
				String password = GetterUtil.getString(
					properties.get("saml.keystore.password"));

				if (Validator.isNotNull(password)) {
					return password;
				}
			}
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to read keystore password from configuration",
					exception);
			}
		}

		return "liferay";
	}

	private void _saveDLKeyStore(
			long companyId, KeyStore keyStore, char[] password, String path)
		throws Exception {

		File tempFile = File.createTempFile("saml-ks", ".p12");

		try {
			try (FileOutputStream fileOutputStream = new FileOutputStream(
					tempFile)) {

				keyStore.store(fileOutputStream, password);
			}

			if (_store.hasFile(
					companyId, CompanyConstants.SYSTEM, path,
					Store.VERSION_DEFAULT)) {

				_store.deleteDirectory(
					companyId, CompanyConstants.SYSTEM, path);
			}

			try (FileInputStream fileInputStream = new FileInputStream(
					tempFile)) {

				_store.addFile(
					companyId, CompanyConstants.SYSTEM, path,
					Store.VERSION_DEFAULT, fileInputStream);
			}
		}
		finally {
			tempFile.delete();
		}
	}

	private void _upgradeConfiguration() throws Exception {
		Configuration configuration = _configurationAdmin.getConfiguration(
			_SAML_CONFIGURATION_PID, StringPool.QUESTION);

		String liferayHome = PropsUtil.get(PropsKeys.LIFERAY_HOME);

		Dictionary<String, Object> properties = configuration.getProperties();

		if (properties == null) {
			_jksFileSystemKeystorePath = liferayHome + "/data/keystore.jks";
			_pkcs12FileSystemKeystorePath = StringUtil.replace(
				SamlConfiguration.KEYSTORE_PATH_DEFAULT, "${liferay.home}",
				liferayHome);

			return;
		}

		String keyStoreType = GetterUtil.getString(
			properties.get("saml.keystore.type"));

		if (StringUtil.equalsIgnoreCase(keyStoreType, "jks")) {
			properties.put("saml.keystore.type", "PKCS12");

			String keyStorePath = GetterUtil.getString(
				properties.get("saml.keystore.path"));

			if (keyStorePath.endsWith(".jks")) {
				_jksFileSystemKeystorePath = StringUtil.replace(
					keyStorePath, "${liferay.home}", liferayHome);

				properties.put(
					"saml.keystore.path",
					keyStorePath.replaceAll("\\.jks$", ".p12"));

				_pkcs12FileSystemKeystorePath = StringUtil.replace(
					GetterUtil.getString(properties.get("saml.keystore.path")),
					"${liferay.home}", liferayHome);
			}

			configuration.update(properties);

			if (_log.isInfoEnabled()) {
				_log.info(
					"Updated SAML configuration: keystore type changed from " +
						"JKS to PKCS12");
			}
		}
	}

	private void _upgradeDLKeystores() {
		String password = _getKeystorePassword();

		_companyLocalService.forEachCompanyId(
			companyId -> {
				char[] passwordChars = password.toCharArray();

				try {
					boolean hasJKSKeystore = _store.hasFile(
						companyId, CompanyConstants.SYSTEM,
						_JKS_DL_KEYSTORE_PATH, Store.VERSION_DEFAULT);

					boolean hasPKCS12Keystore = _store.hasFile(
						companyId, CompanyConstants.SYSTEM,
						_PKCS12_DL_KEYSTORE_PATH, Store.VERSION_DEFAULT);

					if (hasJKSKeystore && !hasPKCS12Keystore) {
						try (InputStream inputStream = _store.getFileAsStream(
								companyId, CompanyConstants.SYSTEM,
								_JKS_DL_KEYSTORE_PATH, Store.VERSION_DEFAULT)) {

							KeyStore pkcs12KeyStore = _convertJKSToPKCS12(
								inputStream, passwordChars);

							_saveDLKeyStore(
								companyId, pkcs12KeyStore, passwordChars,
								_PKCS12_DL_KEYSTORE_PATH);
						}

						_store.deleteDirectory(
							companyId, CompanyConstants.SYSTEM,
							_JKS_DL_KEYSTORE_PATH);

						if (_log.isInfoEnabled()) {
							_log.info(
								"Migrated DL SAML keystore from JKS to " +
									"PKCS12 for company " + companyId);
						}
					}
				}
				catch (NoSuchFileException noSuchFileException) {
					if (_log.isDebugEnabled()) {
						_log.debug(
							StringBundler.concat(
								"No JKS keystore found in Document Library ",
								"for company ", companyId),
							noSuchFileException);
					}
				}
				catch (Exception exception) {
					_log.error(
						"Unable to migrate DL SAML keystore for company " +
							companyId,
						exception);
				}
				finally {
					Arrays.fill(passwordChars, '\0');
				}
			});
	}

	private void _upgradeFileSystemKeystore() {
		File oldFile = new File(_jksFileSystemKeystorePath);
		File newFile = new File(_pkcs12FileSystemKeystorePath);

		String password = _getKeystorePassword();

		char[] passwordChars = password.toCharArray();

		try {
			if (oldFile.exists() && !newFile.exists()) {
				try (FileInputStream fileInputStream = new FileInputStream(
						oldFile)) {

					KeyStore pkcs12KeyStore = _convertJKSToPKCS12(
						fileInputStream, passwordChars);

					File parentDir = newFile.getParentFile();

					if (!parentDir.exists()) {
						parentDir.mkdirs();
					}

					try (FileOutputStream fileOutputStream =
							new FileOutputStream(newFile)) {

						pkcs12KeyStore.store(fileOutputStream, passwordChars);
					}

					if (_log.isInfoEnabled()) {
						_log.info(
							StringBundler.concat(
								"Migrated filesystem SAML keystore from ",
								_jksFileSystemKeystorePath, " (JKS) to ",
								_pkcs12FileSystemKeystorePath, " (PKCS12)"));
					}
				}
			}
		}
		catch (Exception exception) {
			_log.error("Unable to migrate filesystem SAML keystore", exception);
		}
		finally {
			Arrays.fill(passwordChars, '\0');
		}
	}

	private static final String _JKS_DL_KEYSTORE_PATH = "saml/keystore.jks";

	private static final String _PKCS12_DL_KEYSTORE_PATH = "saml/keystore.p12";

	private static final String _SAML_CONFIGURATION_PID =
		"com.liferay.saml.runtime.configuration.SamlConfiguration";

	private static final Log _log = LogFactoryUtil.getLog(
		SamlKeyStoreTypeUpgradeProcess.class);

	private static String _jksFileSystemKeystorePath;
	private static String _pkcs12FileSystemKeystorePath;

	private final CertificateTool _certificateTool;
	private final CompanyLocalService _companyLocalService;
	private final ConfigurationAdmin _configurationAdmin;
	private final Store _store;

}