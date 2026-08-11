/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.security.fips.constants.FIPSConstants;
import com.liferay.portal.security.fips.exception.FIPSSessionTimeoutException;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;
import com.liferay.portal.security.fips.service.FIPSSessionSettingsLocalService;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class FIPSSessionSettingsLocalServiceTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@After
	public void tearDown() throws Exception {
		FIPSSessionSettings fipsSessionSettings =
			_fipsSessionSettingsLocalService.getCompanyFIPSSessionSettings(
				TestPropsValues.getCompanyId());

		if (fipsSessionSettings.getFipsSessionSettingsId() > 0) {
			_fipsSessionSettingsLocalService.deleteFIPSSessionSettings(
				fipsSessionSettings);
		}
	}

	@Test
	public void testGetCompanyFIPSSessionSettingsWhenUnsaved()
		throws Exception {

		FIPSSessionSettings fipsSessionSettings =
			_fipsSessionSettingsLocalService.getCompanyFIPSSessionSettings(
				TestPropsValues.getCompanyId());

		Assert.assertEquals(
			FIPSConstants.SESSION_ABSOLUTE_LIFETIME_DEFAULT_MINUTES,
			fipsSessionSettings.getAbsoluteLifetimeMinutes());
		Assert.assertEquals(
			FIPSConstants.SESSION_IDLE_TIMEOUT_DEFAULT_MINUTES,
			fipsSessionSettings.getIdleTimeoutMinutes());
	}

	@Test
	public void testUpdateCompanyFIPSSessionSettings() throws Exception {
		FIPSSessionSettings fipsSessionSettings =
			_fipsSessionSettingsLocalService.updateCompanyFIPSSessionSettings(
				TestPropsValues.getUserId(), TestPropsValues.getCompanyId(), 30,
				1440);

		Assert.assertEquals(
			1440, fipsSessionSettings.getAbsoluteLifetimeMinutes());
		Assert.assertEquals(30, fipsSessionSettings.getIdleTimeoutMinutes());

		fipsSessionSettings =
			_fipsSessionSettingsLocalService.getCompanyFIPSSessionSettings(
				TestPropsValues.getCompanyId());

		Assert.assertEquals(
			1440, fipsSessionSettings.getAbsoluteLifetimeMinutes());
		Assert.assertEquals(30, fipsSessionSettings.getIdleTimeoutMinutes());
	}

	@Test
	public void testUpdateCompanyFIPSSessionSettingsWhenOutOfRange()
		throws Exception {

		_testUpdateCompanyFIPSSessionSettingsWhenOutOfRange(43201, 15);
		_testUpdateCompanyFIPSSessionSettingsWhenOutOfRange(0, 15);
		_testUpdateCompanyFIPSSessionSettingsWhenOutOfRange(-1, 15);
		_testUpdateCompanyFIPSSessionSettingsWhenOutOfRange(43200, 721);
		_testUpdateCompanyFIPSSessionSettingsWhenOutOfRange(43200, 0);
		_testUpdateCompanyFIPSSessionSettingsWhenOutOfRange(43200, -1);
	}

	private void _testUpdateCompanyFIPSSessionSettingsWhenOutOfRange(
			int absoluteLifetimeMinutes, int idleTimeoutMinutes)
		throws Exception {

		Assert.assertThrows(
			FIPSSessionTimeoutException.class,
			() ->
				_fipsSessionSettingsLocalService.
					updateCompanyFIPSSessionSettings(
						TestPropsValues.getUserId(),
						TestPropsValues.getCompanyId(), idleTimeoutMinutes,
						absoluteLifetimeMinutes));
	}

	@Inject
	private FIPSSessionSettingsLocalService _fipsSessionSettingsLocalService;

}