/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.impl;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.security.fips.constants.FIPSConstants;
import com.liferay.portal.security.fips.exception.FIPSSessionTimeoutException;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;
import com.liferay.portal.security.fips.service.base.FIPSSessionSettingsLocalServiceBaseImpl;

import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Carries no permission checks, so that the session enforcement components can
 * read the settings while running as whichever user issued the request. Writes
 * reaching an operator go through {@link
 * com.liferay.portal.security.fips.service.impl.FIPSSessionSettingsServiceImpl}
 * instead, which requires the Crypto Officer role.
 *
 * @author Manuele Castro
 */
@Component(
	property = "model.class.name=com.liferay.portal.security.fips.model.FIPSSessionSettings",
	service = AopService.class
)
public class FIPSSessionSettingsLocalServiceImpl
	extends FIPSSessionSettingsLocalServiceBaseImpl {

	/**
	 * Returns the settings of the company, or a transient instance carrying the
	 * deployment defaults when the company has never saved any. Callers are
	 * therefore free of null checks.
	 */
	@Override
	public FIPSSessionSettings getCompanyFIPSSessionSettings(long companyId) {
		FIPSSessionSettings fipsSessionSettings =
			fipsSessionSettingsPersistence.fetchByCompanyId(companyId);

		if (fipsSessionSettings != null) {
			return fipsSessionSettings;
		}

		fipsSessionSettings = fipsSessionSettingsPersistence.create(0);

		fipsSessionSettings.setCompanyId(companyId);
		fipsSessionSettings.setAbsoluteLifetimeMinutes(
			FIPSConstants.SESSION_ABSOLUTE_LIFETIME_DEFAULT_MINUTES);
		fipsSessionSettings.setIdleTimeoutMinutes(
			FIPSConstants.SESSION_IDLE_TIMEOUT_DEFAULT_MINUTES);

		return fipsSessionSettings;
	}

	@Override
	public FIPSSessionSettings updateCompanyFIPSSessionSettings(
			long userId, long companyId, int idleTimeoutMinutes,
			int absoluteLifetimeMinutes)
		throws PortalException {

		_validate(absoluteLifetimeMinutes, idleTimeoutMinutes);

		FIPSSessionSettings fipsSessionSettings =
			fipsSessionSettingsPersistence.fetchByCompanyId(companyId);

		if (fipsSessionSettings == null) {
			fipsSessionSettings = fipsSessionSettingsPersistence.create(
				counterLocalService.increment());

			fipsSessionSettings.setCompanyId(companyId);
			fipsSessionSettings.setCreateDate(new Date());
		}

		User user = _userLocalService.getUser(userId);

		fipsSessionSettings.setUserId(userId);
		fipsSessionSettings.setUserName(user.getFullName());
		fipsSessionSettings.setModifiedDate(new Date());
		fipsSessionSettings.setAbsoluteLifetimeMinutes(absoluteLifetimeMinutes);
		fipsSessionSettings.setIdleTimeoutMinutes(idleTimeoutMinutes);

		return fipsSessionSettingsPersistence.update(fipsSessionSettings);
	}

	private void _validate(int absoluteLifetimeMinutes, int idleTimeoutMinutes)
		throws FIPSSessionTimeoutException {

		if ((absoluteLifetimeMinutes <= 0) ||
			(absoluteLifetimeMinutes >
				FIPSConstants.SESSION_ABSOLUTE_LIFETIME_MAX_MINUTES)) {

			throw new FIPSSessionTimeoutException(
				StringBundler.concat(
					"The absolute session lifetime must be between 1 and ",
					FIPSConstants.SESSION_ABSOLUTE_LIFETIME_MAX_MINUTES,
					" minutes, but was ", absoluteLifetimeMinutes));
		}

		if ((idleTimeoutMinutes <= 0) ||
			(idleTimeoutMinutes >
				FIPSConstants.SESSION_IDLE_TIMEOUT_MAX_MINUTES)) {

			throw new FIPSSessionTimeoutException(
				StringBundler.concat(
					"The session idle timeout must be between 1 and ",
					FIPSConstants.SESSION_IDLE_TIMEOUT_MAX_MINUTES,
					" minutes, but was ", idleTimeoutMinutes));
		}
	}

	@Reference
	private UserLocalService _userLocalService;

}