/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.internal.configuration.persistence.listener.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListenerException;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.security.fips.configuration.FIPSSessionConfiguration;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Dictionary;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Proves that the FIPS session timeouts are writable only by a Crypto Officer.
 *
 * <p>
 * The listener is the only thing standing between a company administrator and
 * the configuration, because hiding the entry from System Settings does not
 * stop a direct save, so these assertions carry the acceptance criterion.
 * </p>
 *
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class FIPSSessionConfigurationModelListenerTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() {
		_name = PrincipalThreadLocal.getName();
		_permissionChecker = PermissionThreadLocal.getPermissionChecker();
	}

	@After
	public void tearDown() throws Exception {
		PermissionThreadLocal.setPermissionChecker(_permissionChecker);
		PrincipalThreadLocal.setName(_name);

		_configurationProvider.deleteCompanyConfiguration(
			FIPSSessionConfiguration.class, TestPropsValues.getCompanyId());
	}

	@Test
	public void testSaveAsAdministrator() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			_registerPortalInstance();

			UserTestUtil.setUser(TestPropsValues.getUser());

			_assertRefused(_toProperties(1440, 30));
		}
	}

	@Test
	public void testSaveAsCryptoOfficer() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			_setUpCryptoOfficer();

			_configurationProvider.saveCompanyConfiguration(
				FIPSSessionConfiguration.class, TestPropsValues.getCompanyId(),
				_toProperties(1440, 30));

			FIPSSessionConfiguration fipsSessionConfiguration =
				_configurationProvider.getCompanyConfiguration(
					FIPSSessionConfiguration.class,
					TestPropsValues.getCompanyId());

			Assert.assertEquals(
				1440, fipsSessionConfiguration.absoluteLifetimeMinutes());
			Assert.assertEquals(
				30, fipsSessionConfiguration.idleTimeoutMinutes());
		}
	}

	@Test
	public void testSaveOutOfRangeAsCryptoOfficer() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			_setUpCryptoOfficer();

			_assertRefused(_toProperties(43201, 15));
			_assertRefused(_toProperties(0, 15));
			_assertRefused(_toProperties(-1, 15));
			_assertRefused(_toProperties(43200, 721));
			_assertRefused(_toProperties(43200, 0));
			_assertRefused(_toProperties(43200, -1));
		}
	}

	/**
	 * A write carrying no authenticated user stands in for a
	 * <code>.config</code> file deploy, a Gogo <code>config:update</code>, and
	 * any bundle saving programmatically. All of them are refused.
	 */
	@Test
	public void testSaveWithoutAuthenticatedUser() throws Exception {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			_registerPortalInstance();

			PermissionThreadLocal.setPermissionChecker(null);
			PrincipalThreadLocal.setName((String)null);

			_assertRefused(_toProperties(1440, 30));
		}
	}

	private void _assertRefused(Dictionary<String, Object> properties)
		throws Exception {

		Exception exception = Assert.assertThrows(
			Exception.class,
			() -> _configurationProvider.saveCompanyConfiguration(
				FIPSSessionConfiguration.class, TestPropsValues.getCompanyId(),
				properties));

		Assert.assertTrue(_hasConfigurationModelListenerException(exception));
	}

	private boolean _hasConfigurationModelListenerException(
		Throwable throwable) {

		while (throwable != null) {
			if (throwable instanceof ConfigurationModelListenerException) {
				return true;
			}

			throwable = throwable.getCause();
		}

		return false;
	}

	private Company _registerPortalInstance() throws Exception {
		Company company = _companyLocalService.getCompany(
			TestPropsValues.getCompanyId());

		_portalInstanceLifecycleListener.portalInstanceRegistered(company);

		return company;
	}

	private void _setUpCryptoOfficer() throws Exception {
		Company company = _registerPortalInstance();

		Role role = _roleLocalService.fetchRole(
			company.getCompanyId(), RoleConstants.CRYPTO_OFFICER);

		User user = UserTestUtil.addUser(company);

		_roleLocalService.addUserRoles(
			user.getUserId(), new long[] {role.getRoleId()});

		UserTestUtil.setUser(user);
	}

	private Dictionary<String, Object> _toProperties(
		int absoluteLifetimeMinutes, int idleTimeoutMinutes) {

		return HashMapDictionaryBuilder.<String, Object>put(
			"absoluteLifetimeMinutes", absoluteLifetimeMinutes
		).put(
			"idleTimeoutMinutes", idleTimeoutMinutes
		).build();
	}

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private ConfigurationProvider _configurationProvider;

	private String _name;
	private PermissionChecker _permissionChecker;

	@Inject(
		filter = "component.name=com.liferay.portal.security.fips.internal.instance.lifecycle.FIPSPortalInstanceLifecycleListener"
	)
	private PortalInstanceLifecycleListener _portalInstanceLifecycleListener;

	@Inject
	private RoleLocalService _roleLocalService;

}