/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.impl;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;
import com.liferay.portal.security.fips.service.base.FIPSSessionSettingsServiceBaseImpl;
import com.liferay.portal.security.fips.util.FIPSUtil;

import org.osgi.service.component.annotations.Component;

/**
 * Restricts the FIPS session settings to the Crypto Officer. The check is role
 * membership rather than a resource permission, so a company administrator and
 * an omniadmin are refused as well, even though both pass every permission
 * lookup.
 *
 * @author Manuele Castro
 */
@Component(
	property = {
		"json.web.service.context.name=fips",
		"json.web.service.context.path=FIPSSessionSettings"
	},
	service = AopService.class
)
public class FIPSSessionSettingsServiceImpl
	extends FIPSSessionSettingsServiceBaseImpl {

	@Override
	public FIPSSessionSettings getCompanyFIPSSessionSettings(long companyId)
		throws PortalException {

		_checkCryptoOfficerRole();

		return fipsSessionSettingsLocalService.getCompanyFIPSSessionSettings(
			companyId);
	}

	@Override
	public FIPSSessionSettings updateCompanyFIPSSessionSettings(
			long companyId, int idleTimeoutMinutes, int absoluteLifetimeMinutes)
		throws PortalException {

		_checkCryptoOfficerRole();

		return fipsSessionSettingsLocalService.updateCompanyFIPSSessionSettings(
			getUserId(), companyId, idleTimeoutMinutes,
			absoluteLifetimeMinutes);
	}

	private void _checkCryptoOfficerRole() throws PortalException {
		if (!FIPSUtil.hasCryptoOfficerRole(getUser())) {
			throw new PrincipalException(
				StringBundler.concat(
					"User ", getUserId(), " must have the role \"",
					RoleConstants.CRYPTO_OFFICER,
					"\" to read or update the FIPS session settings"));
		}
	}

}