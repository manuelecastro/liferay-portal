/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;
import com.liferay.portal.security.fips.service.FIPSSessionSettingsLocalService;
import com.liferay.portal.security.fips.service.FIPSSessionSettingsService;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Proves that configuring the FIPS session settings is restricted to the
 * Crypto Officer, which is the acceptance criterion the remote service exists
 * to satisfy.
 *
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class FIPSSessionSettingsServiceTest {

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

		FIPSSessionSettings fipsSessionSettings =
			_fipsSessionSettingsLocalService.getCompanyFIPSSessionSettings(
				TestPropsValues.getCompanyId());

		if (fipsSessionSettings.getFipsSessionSettingsId() > 0) {
			_fipsSessionSettingsLocalService.deleteFIPSSessionSettings(
				fipsSessionSettings);
		}
	}

	@Test
	public void testUpdateCompanyFIPSSessionSettingsAsAdministrator()
		throws Exception {

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			_registerPortalInstance();

			UserTestUtil.setUser(TestPropsValues.getUser());

			Assert.assertThrows(
				PrincipalException.class,
				() ->
					_fipsSessionSettingsService.
						updateCompanyFIPSSessionSettings(
							TestPropsValues.getCompanyId(), 30, 1440));
		}
	}

	@Test
	public void testUpdateCompanyFIPSSessionSettingsAsCryptoOfficer()
		throws Exception {

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			Company company = _registerPortalInstance();

			Role role = _roleLocalService.fetchRole(
				company.getCompanyId(), RoleConstants.CRYPTO_OFFICER);

			User user = UserTestUtil.addUser(company);

			_roleLocalService.addUserRoles(
				user.getUserId(), new long[] {role.getRoleId()});

			UserTestUtil.setUser(user);

			FIPSSessionSettings fipsSessionSettings =
				_fipsSessionSettingsService.updateCompanyFIPSSessionSettings(
					company.getCompanyId(), 30, 1440);

			Assert.assertEquals(
				1440, fipsSessionSettings.getAbsoluteLifetimeMinutes());
			Assert.assertEquals(
				30, fipsSessionSettings.getIdleTimeoutMinutes());
		}
	}

	private Company _registerPortalInstance() throws Exception {
		Company company = _companyLocalService.getCompany(
			TestPropsValues.getCompanyId());

		_portalInstanceLifecycleListener.portalInstanceRegistered(company);

		return company;
	}

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private FIPSSessionSettingsLocalService _fipsSessionSettingsLocalService;

	@Inject
	private FIPSSessionSettingsService _fipsSessionSettingsService;

	private String _name;
	private PermissionChecker _permissionChecker;

	@Inject(
		filter = "component.name=com.liferay.portal.security.fips.internal.instance.lifecycle.FIPSPortalInstanceLifecycleListener"
	)
	private PortalInstanceLifecycleListener _portalInstanceLifecycleListener;

	@Inject
	private RoleLocalService _roleLocalService;

}