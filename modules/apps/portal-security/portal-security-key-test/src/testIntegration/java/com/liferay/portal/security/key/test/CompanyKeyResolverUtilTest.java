/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.key.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.test.util.ConfigurationTemporarySwapper;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.dao.orm.EntityCacheUtil;
import com.liferay.portal.kernel.encryptor.CompanyKeyResolverUtil;
import com.liferay.portal.kernel.encryptor.EncryptorUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.security.key.spi.crypto.CryptoProvider;
import com.liferay.portal.security.key.test.util.TestCompanyCryptoProvider;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.security.Key;

import java.util.Arrays;
import java.util.Dictionary;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Christopher Kian
 */
@RunWith(Arquillian.class)
public class CompanyKeyResolverUtilTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_keyManagerConfigurationTemporarySwapper =
			new ConfigurationTemporarySwapper(
				_KEY_MANAGER_CONFIGURATION_PID,
				_getKeyManagerConfigurationProperties(
					TestCompanyCryptoProvider.KEY_IDENTIFIER));
		_keyManagerCustomProfileConfigurationTemporarySwapper =
			new ConfigurationTemporarySwapper(
				_KEY_MANAGER_CUSTOM_PROFILE_CONFIGURATION_PID,
				_getKeyManagerCustomProfileConfigurationProperties(
					TestCompanyCryptoProvider.PROVIDER_ID));

		Bundle bundle = FrameworkUtil.getBundle(
			CompanyKeyResolverUtilTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_testCompanyCryptoProvider = new TestCompanyCryptoProvider();

		_serviceRegistration = bundleContext.registerService(
			CryptoProvider.class, _testCompanyCryptoProvider,
			HashMapDictionaryBuilder.<String, Object>put(
				"crypto.provider.id", TestCompanyCryptoProvider.PROVIDER_ID
			).build());
	}

	@After
	public void tearDown() throws Exception {
		_keyManagerConfigurationTemporarySwapper.close();
		_keyManagerCustomProfileConfigurationTemporarySwapper.close();

		_serviceRegistration.unregister();
	}

	@Test
	public void testCheckCompanyKey() throws Exception {
		_company = CompanyTestUtil.addCompany();

		String keyString = _company.getKey();

		Assert.assertTrue(CompanyKeyResolverUtil.isWrappedKey(keyString));
		Assert.assertFalse(CompanyKeyResolverUtil.isFIPSKey(keyString));

		_saveKeyManagerConfiguration(StringPool.BLANK);

		_legacyCompany = CompanyTestUtil.addCompany();

		String legacyKeyString = _legacyCompany.getKey();

		Assert.assertFalse(
			CompanyKeyResolverUtil.isWrappedKey(legacyKeyString));
		Assert.assertFalse(CompanyKeyResolverUtil.isFIPSKey(legacyKeyString));

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				_CLASS_NAME_FIPS_LOG4J_UTIL, LoggerTestUtil.INFO)) {

			// A wrapped key is never replaced with a plaintext one

			Assert.assertEquals(keyString, _checkCompanyKey(_company));

			// An unreachable KEK leaves the key untouched

			_saveKeyManagerConfiguration(
				TestCompanyCryptoProvider.KEY_IDENTIFIER);
			_saveKeyManagerCustomProfileConfiguration(
				RandomTestUtil.randomString());

			Assert.assertEquals(
				legacyKeyString, _checkCompanyKey(_legacyCompany));

			// With a KEK, the key is rotated and wrapped

			_saveKeyManagerCustomProfileConfiguration(
				TestCompanyCryptoProvider.PROVIDER_ID);

			String rotatedKeyString = _checkCompanyKey(_company);

			Assert.assertTrue(
				CompanyKeyResolverUtil.isFIPSKey(rotatedKeyString));
			Assert.assertTrue(
				CompanyKeyResolverUtil.isWrappedKey(rotatedKeyString));

			Key key = CompanyKeyResolverUtil.unwrapKey(
				_company.getCompanyId(), rotatedKeyString);

			byte[] encodedBytes = key.getEncoded();

			Assert.assertEquals(
				Arrays.toString(encodedBytes), 32, encodedBytes.length);

			// A rotated key is kept

			Assert.assertEquals(rotatedKeyString, _checkCompanyKey(_company));

			// Without a KEK, the key is rotated in plaintext

			_saveKeyManagerConfiguration(StringPool.BLANK);

			String rotatedLegacyKeyString = _checkCompanyKey(_legacyCompany);

			Assert.assertTrue(
				CompanyKeyResolverUtil.isFIPSKey(rotatedLegacyKeyString));
			Assert.assertFalse(
				CompanyKeyResolverUtil.isWrappedKey(rotatedLegacyKeyString));

			key = CompanyKeyResolverUtil.unwrapKey(
				_legacyCompany.getCompanyId(), rotatedLegacyKeyString);

			encodedBytes = key.getEncoded();

			Assert.assertEquals(
				Arrays.toString(encodedBytes), 32, encodedBytes.length);

			Assert.assertEquals(
				1, _countFIPSMigrationEvents(logCapture, _company, "failed"));
			Assert.assertEquals(
				1, _countFIPSMigrationEvents(logCapture, _company, "migrated"));
			Assert.assertEquals(
				1,
				_countFIPSMigrationEvents(
					logCapture, _legacyCompany, "failed"));
			Assert.assertEquals(
				1,
				_countFIPSMigrationEvents(
					logCapture, _legacyCompany, "migrated"));
		}
	}

	@Test
	public void testUnwrapKey() throws Exception {
		long companyId = RandomTestUtil.randomLong();
		Key key = EncryptorUtil.generateKey();

		String keyString = CompanyKeyResolverUtil.wrapKey(companyId, key);

		_saveKeyManagerConfiguration(TestCompanyCryptoProvider.KEY_IDENTIFIER);

		int decryptCount = _testCompanyCryptoProvider.getDecryptCount();
		Key unwrappedKey = CompanyKeyResolverUtil.unwrapKey(
			companyId, keyString);

		Assert.assertEquals(
			decryptCount + 1, _testCompanyCryptoProvider.getDecryptCount());
		Assert.assertEquals(key, unwrappedKey);

		unwrappedKey = CompanyKeyResolverUtil.unwrapKey(companyId, keyString);

		Assert.assertEquals(
			decryptCount + 1, _testCompanyCryptoProvider.getDecryptCount());
		Assert.assertEquals(key, unwrappedKey);

		key = EncryptorUtil.generateKey();

		unwrappedKey = CompanyKeyResolverUtil.unwrapKey(
			companyId, EncryptorUtil.serializeKey(key));

		Assert.assertEquals(
			decryptCount + 1, _testCompanyCryptoProvider.getDecryptCount());
		Assert.assertEquals(key, unwrappedKey);
	}

	@Test
	public void testWrapKey() throws Exception {
		_company = CompanyTestUtil.addCompany();

		Assert.assertTrue(
			CompanyKeyResolverUtil.isWrappedKey(_company.getKey()));

		Key key = _company.getKeyObj();

		Assert.assertNotNull(key);

		String plaintext = RandomTestUtil.randomString();

		Assert.assertEquals(
			plaintext,
			EncryptorUtil.decrypt(key, EncryptorUtil.encrypt(key, plaintext)));

		EntityCacheUtil.clearCache();

		Company persistedCompany = CompanyLocalServiceUtil.getCompany(
			_company.getCompanyId());

		Assert.assertTrue(
			CompanyKeyResolverUtil.isWrappedKey(persistedCompany.getKey()));
		Assert.assertEquals(key, persistedCompany.getKeyObj());

		_saveKeyManagerConfiguration(StringPool.BLANK);

		_legacyCompany = CompanyTestUtil.addCompany();

		String keyString = _legacyCompany.getKey();

		Assert.assertFalse(CompanyKeyResolverUtil.isWrappedKey(keyString));
		Assert.assertEquals(
			EncryptorUtil.deserializeKey(keyString),
			_legacyCompany.getKeyObj());
	}

	private String _checkCompanyKey(Company company) throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			CompanyLocalServiceUtil.checkCompanyKey(company.getCompanyId());
		}

		EntityCacheUtil.clearCache();

		Company persistedCompany = CompanyLocalServiceUtil.getCompany(
			company.getCompanyId());

		return persistedCompany.getKey();
	}

	private int _countFIPSMigrationEvents(
		LogCapture logCapture, Company company, String result) {

		int count = 0;

		for (LogEntry logEntry : logCapture.getLogEntries()) {
			String message = logEntry.getMessage();

			if (!message.contains("company-id=" + company.getCompanyId()) ||
				!message.contains("event-type=fips-migration") ||
				!message.contains("item=company-key") ||
				!message.contains("result=" + result)) {

				continue;
			}

			count++;
		}

		return count;
	}

	private Dictionary<String, Object> _getKeyManagerConfigurationProperties(
		String companyKEKIdentifier) {

		return HashMapDictionaryBuilder.<String, Object>put(
			"activeProfileId", "custom"
		).put(
			"companyKEKIdentifier", companyKEKIdentifier
		).build();
	}

	private Dictionary<String, Object>
		_getKeyManagerCustomProfileConfigurationProperties(
			String companyKEKProviderId) {

		return HashMapDictionaryBuilder.<String, Object>put(
			"companyKEKProviderId", companyKEKProviderId
		).build();
	}

	private void _saveKeyManagerConfiguration(String companyKEKIdentifier)
		throws Exception {

		ConfigurationTestUtil.saveConfiguration(
			_KEY_MANAGER_CONFIGURATION_PID,
			_getKeyManagerConfigurationProperties(companyKEKIdentifier));
	}

	private void _saveKeyManagerCustomProfileConfiguration(
			String companyKEKProviderId)
		throws Exception {

		ConfigurationTestUtil.saveConfiguration(
			_KEY_MANAGER_CUSTOM_PROFILE_CONFIGURATION_PID,
			_getKeyManagerCustomProfileConfigurationProperties(
				companyKEKProviderId));
	}

	private static final String _CLASS_NAME_FIPS_LOG4J_UTIL =
		"com.liferay.portal.kernel.internal.log4j.FIPSLog4jUtil";

	private static final String _KEY_MANAGER_CONFIGURATION_PID =
		"com.liferay.portal.security.key.internal.profile.configuration." +
			"KeyManagerConfiguration";

	private static final String _KEY_MANAGER_CUSTOM_PROFILE_CONFIGURATION_PID =
		"com.liferay.portal.security.key.internal.profile.configuration." +
			"KeyManagerCustomProfileConfiguration";

	@DeleteAfterTestRun
	private Company _company;

	private ConfigurationTemporarySwapper
		_keyManagerConfigurationTemporarySwapper;
	private ConfigurationTemporarySwapper
		_keyManagerCustomProfileConfigurationTemporarySwapper;

	@DeleteAfterTestRun
	private Company _legacyCompany;

	private ServiceRegistration<CryptoProvider> _serviceRegistration;
	private TestCompanyCryptoProvider _testCompanyCryptoProvider;

}