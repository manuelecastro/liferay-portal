/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.saml.internal.upgrade.v2_0_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.store.Store;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;
import com.liferay.saml.runtime.certificate.CertificateEntityId;
import com.liferay.saml.runtime.certificate.CertificateTool;
import com.liferay.saml.runtime.configuration.SamlConfiguration;
import com.liferay.saml.runtime.configuration.SamlProviderConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Rafael Praxedes
 */
@RunWith(Arquillian.class)
public class SamlKeyStoreTypeUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_upgradeProcess = UpgradeTestUtil.getUpgradeStep(
			_upgradeStepRegistrator, _CLASS_NAME);

		_companyId = TestPropsValues.getCompanyId();
	}

	@After
	public void tearDown() throws Exception {
		_deleteDLKeystore();
		_deleteFileSystemKeystores();

		if (_configuration != null) {
			ConfigurationTestUtil.deleteConfiguration(_configuration);
		}

		if (_pid != null) {
			ConfigurationTestUtil.deleteConfiguration(_pid);
		}
	}

	@Test
	public void testUpgrade() throws Exception {
		String entityId = RandomTestUtil.randomString();

		String encryptionAlias = entityId + "-encryption";

		String credentialPassword = RandomTestUtil.randomString();
		String encryptionCredentialPassword = RandomTestUtil.randomString();
		String keystorePassword = RandomTestUtil.randomString();

		_store.addFile(
			_companyId, CompanyConstants.SYSTEM, _JKS_DL_KEYSTORE_PATH,
			Store.VERSION_DEFAULT,
			new ByteArrayInputStream(
				_createJKSKeystoreBytes(
					encryptionAlias, encryptionCredentialPassword, keystorePassword)));
		_store.addFile(
			_companyId, CompanyConstants.SYSTEM, _JKS_DL_KEYSTORE_PATH,
			Store.VERSION_DEFAULT,
			new ByteArrayInputStream(
				_createJKSKeystoreBytes(
					entityId, credentialPassword, keystorePassword)));

		_setSamlConfiguration("${liferay.home}/data/test/keystore.jks", "jks", keystorePassword);

		_pid = ConfigurationTestUtil.createFactoryConfiguration(
			SamlProviderConfiguration.class.getName(),
			HashMapDictionaryBuilder.<String, Object>put(
				"saml.entity.id", entityId
			).put(
				"saml.keystore.credential.password", credentialPassword
			).put(
				"saml.keystore.encryption.credential.password", encryptionCredentialPassword
			).build());

		_upgradeProcess.upgrade();

		_assertSamlConfiguration(
			"${liferay.home}/data/test/keystore.p12", "PKCS12", keystorePassword);

		Assert.assertTrue(
			_store.hasFile(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT));

		try (InputStream inputStream = _store.getFileAsStream(
			_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
			Store.VERSION_DEFAULT)) {

			KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

			pkcs12KeyStore.load(inputStream, keystorePassword.toCharArray());

			Assert.assertTrue(pkcs12KeyStore.containsAlias(encryptionAlias));
			Assert.assertTrue(pkcs12KeyStore.containsAlias(entityId));
		}
	}

	@Test
	public void testUpgradeConfigurationFromJKSToPKCS12() throws Exception {
		_setSamlConfiguration(
			"${liferay.home}/data/test/keystore.jks", "jks", null);

		_upgradeProcess.upgrade();

		_assertSamlConfiguration(
			"${liferay.home}/data/test/keystore.p12", "PKCS12", null);
	}

	@Test
	public void testUpgradeDLKeystoreEncryptionKey() throws Exception {
		String entityId = RandomTestUtil.randomString();

		String encryptionAlias = entityId + "-encryption";

		String credentialPassword = RandomTestUtil.randomString();

		String keystorePassword = RandomTestUtil.randomString();

		_store.addFile(
			_companyId, CompanyConstants.SYSTEM, _JKS_DL_KEYSTORE_PATH,
			Store.VERSION_DEFAULT,
			new ByteArrayInputStream(
				_createJKSKeystoreBytes(
					encryptionAlias, credentialPassword, keystorePassword)));

		_setSamlConfiguration(null, "jks", keystorePassword);

		_pid = ConfigurationTestUtil.createFactoryConfiguration(
			SamlProviderConfiguration.class.getName(),
			HashMapDictionaryBuilder.<String, Object>put(
				"saml.entity.id", entityId
			).put(
				"saml.keystore.encryption.credential.password", credentialPassword
			).build());

		_upgradeProcess.upgrade();

		Assert.assertTrue(
			_store.hasFile(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT));

		try (InputStream inputStream = _store.getFileAsStream(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT)) {

			KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

			pkcs12KeyStore.load(inputStream, keystorePassword.toCharArray());

			Assert.assertTrue(pkcs12KeyStore.containsAlias(encryptionAlias));
		}
	}

	@Test
	public void testUpgradeDLKeystoreJKSToPKCS12() throws Exception {
		String alias = RandomTestUtil.randomString();
		String credentialPassword = RandomTestUtil.randomString();

		String password = RandomTestUtil.randomString();

		_store.addFile(
			_companyId, CompanyConstants.SYSTEM, _JKS_DL_KEYSTORE_PATH,
			Store.VERSION_DEFAULT,
			new ByteArrayInputStream(
				_createJKSKeystoreBytes(alias, credentialPassword, password)));

		_setSamlConfiguration(null, "jks", password);

		_pid = ConfigurationTestUtil.createFactoryConfiguration(
			SamlProviderConfiguration.class.getName(),
			HashMapDictionaryBuilder.<String, Object>put(
				"saml.entity.id", alias
			).put(
				"saml.keystore.credential.password", credentialPassword
			).build());

		_upgradeProcess.upgrade();

		Assert.assertTrue(
			_store.hasFile(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT));

		try (InputStream inputStream = _store.getFileAsStream(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT)) {

			KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

			pkcs12KeyStore.load(inputStream, password.toCharArray());

			Assert.assertTrue(pkcs12KeyStore.containsAlias(alias));
		}
	}

	@Test
	public void testUpgradeDLKeystoreNeitherExists() throws Exception {
		_setSamlConfiguration(null, "PKCS12", null);

		_upgradeProcess.upgrade();

		Assert.assertFalse(
			_store.hasFile(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT));
	}

	@Test
	public void testUpgradeDLKeystoreSkipsKeyWithNoProviderConfig()
		throws Exception {

		String alias = RandomTestUtil.randomString();
		String password = RandomTestUtil.randomString();

		_store.addFile(
			_companyId, CompanyConstants.SYSTEM, _JKS_DL_KEYSTORE_PATH,
			Store.VERSION_DEFAULT,
			new ByteArrayInputStream(
				_createJKSKeystoreBytes(
					alias, RandomTestUtil.randomString(), password)));

		_setSamlConfiguration(null, "jks", password);

		_upgradeProcess.upgrade();

		Assert.assertFalse(
			_store.hasFile(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT));
	}

	@Test
	public void testUpgradeFileSystemKeystoreJKSToPKCS12() throws Exception {
		String alias = RandomTestUtil.randomString();
		String credentialPassword = RandomTestUtil.randomString();
		String password = RandomTestUtil.randomString();

		String liferayHome = PropsUtil.get(PropsKeys.LIFERAY_HOME);

		File dataDir = new File(liferayHome, "data");

		dataDir.mkdirs();

		try (FileOutputStream fileOutputStream = new FileOutputStream(
				new File(dataDir, "keystore.jks"))) {

			fileOutputStream.write(
				_createJKSKeystoreBytes(alias, credentialPassword, password));
		}

		_pid = ConfigurationTestUtil.createFactoryConfiguration(
			SamlProviderConfiguration.class.getName(),
			HashMapDictionaryBuilder.<String, Object>put(
				"saml.entity.id", alias
			).put(
				"saml.keystore.credential.password", credentialPassword
			).build());

		_setSamlConfiguration(_JKS_FILE_SYSTEM_KEYSTORE_PATH, "jks", password);

		_upgradeProcess.upgrade();

		File p12File = new File(dataDir, "keystore.p12");

		Assert.assertTrue(p12File.exists());

		KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");

		try (FileInputStream fileInputStream = new FileInputStream(p12File)) {
			pkcs12KeyStore.load(fileInputStream, password.toCharArray());
		}

		Assert.assertTrue(pkcs12KeyStore.containsAlias(alias));
	}

	private void _assertSamlConfiguration(
			String keyStorePath, String keyStoreType, String keyStorePassword)
		throws Exception {

		_configuration = _configurationAdmin.getConfiguration(
			SamlConfiguration.class.getName(), StringPool.QUESTION);

		Dictionary<String, Object> properties = _configuration.getProperties();

		if (Validator.isNotNull(keyStorePath)) {
			Assert.assertEquals(
				keyStorePath, properties.get("saml.keystore.path"));
		}

		if (Validator.isNotNull(keyStoreType)) {
			Assert.assertEquals(
				keyStoreType, properties.get("saml.keystore.type"));
		}

		if (Validator.isNotNull(keyStorePassword)) {
			Assert.assertEquals(
				keyStorePassword, properties.get("saml.keystore.password"));
		}
	}

	private byte[] _createJKSKeystoreBytes(
			String alias, String keyPassword, String keystorePassword)
		throws Exception {

		KeyStore jksKeyStore = KeyStore.getInstance("JKS");

		jksKeyStore.load(null, null);

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

		keyPairGenerator.initialize(2048);

		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		CertificateEntityId certificateEntityId = new CertificateEntityId(
			"Test", null, null, null, null, null);

		Calendar startDate = Calendar.getInstance();

		Calendar endDate = (Calendar)startDate.clone();

		endDate.add(Calendar.DAY_OF_YEAR, 365);

		X509Certificate x509Certificate = _certificateTool.generateCertificate(
			keyPair, certificateEntityId, certificateEntityId,
			startDate.getTime(), endDate.getTime(), "SHA256withRSA");

		jksKeyStore.setKeyEntry(
			alias, keyPair.getPrivate(), keyPassword.toCharArray(),
			new X509Certificate[] {x509Certificate});

		ByteArrayOutputStream byteArrayOutputStream =
			new ByteArrayOutputStream();

		jksKeyStore.store(
			byteArrayOutputStream, keystorePassword.toCharArray());

		return byteArrayOutputStream.toByteArray();
	}

	private void _deleteDLKeystore() {
		if (_store.hasFile(
				_companyId, CompanyConstants.SYSTEM, _JKS_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT)) {

			_store.deleteDirectory(
				_companyId, CompanyConstants.SYSTEM, _JKS_DL_KEYSTORE_PATH);
		}

		if (_store.hasFile(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH,
				Store.VERSION_DEFAULT)) {

			_store.deleteDirectory(
				_companyId, CompanyConstants.SYSTEM, _PKCS12_DL_KEYSTORE_PATH);
		}
	}

	private void _deleteFileSystemKeystores() {
		String liferayHome = PropsUtil.get(PropsKeys.LIFERAY_HOME);

		File dataDir = new File(liferayHome, "data");

		File jksFile = new File(dataDir, "keystore.jks");

		if (jksFile.exists()) {
			jksFile.delete();
		}

		File p12File = new File(dataDir, "keystore.p12");

		if (p12File.exists()) {
			p12File.delete();
		}
	}

	private void _setSamlConfiguration(
			String keyStorePath, String keyStoreType, String keyStorePassword)
		throws Exception {

		_configuration = _configurationAdmin.getConfiguration(
			SamlConfiguration.class.getName(), StringPool.QUESTION);

		Dictionary<String, Object> properties = new Hashtable<>();

		if (Validator.isNotNull(keyStorePassword)) {
			properties.put("saml.keystore.password", keyStorePassword);
		}

		properties.put("saml.keystore.type", keyStoreType);

		if (Validator.isNotNull(keyStorePath)) {
			properties.put("saml.keystore.path", keyStorePath);
		}

		_configuration.update(properties);

		_assertSamlConfiguration(keyStorePath, keyStoreType, keyStorePassword);
	}

	private static final String _CLASS_NAME =
		"com.liferay.saml.internal.upgrade.v2_0_0." +
			"SamlKeyStoreTypeUpgradeProcess";

	private static final String _JKS_DL_KEYSTORE_PATH = "saml/keystore.jks";

	private static final String _JKS_FILE_SYSTEM_KEYSTORE_PATH =
		"${liferay.home}/data/keystore.jks";

	private static final String _PKCS12_DL_KEYSTORE_PATH = "saml/keystore.p12";

	@Inject
	private CertificateTool _certificateTool;

	private long _companyId;
	private Configuration _configuration;

	@Inject
	private ConfigurationAdmin _configurationAdmin;

	private String _pid;

	@Inject(
		filter = "(&(objectClass=com.liferay.document.library.kernel.store.Store)(default=true))"
	)
	private Store _store;

	private UpgradeProcess _upgradeProcess;

	@Inject(
		filter = "(&(component.name=com.liferay.saml.internal.upgrade.registry.SamlImplUpgradeStepRegistrator))"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}