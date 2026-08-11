/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;

/**
 * Provides the remote service utility for FIPSSessionSettings. This utility wraps
 * <code>com.liferay.portal.security.fips.service.impl.FIPSSessionSettingsServiceImpl</code> and is an
 * access point for service operations in application layer code running on a
 * remote server. Methods of this service are expected to have security checks
 * based on the propagated JAAS credentials because this service can be
 * accessed remotely.
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettingsService
 * @generated
 */
public class FIPSSessionSettingsServiceUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Add custom service methods to <code>com.liferay.portal.security.fips.service.impl.FIPSSessionSettingsServiceImpl</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static FIPSSessionSettings getCompanyFIPSSessionSettings(
			long companyId)
		throws PortalException {

		return getService().getCompanyFIPSSessionSettings(companyId);
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	public static String getOSGiServiceIdentifier() {
		return getService().getOSGiServiceIdentifier();
	}

	public static FIPSSessionSettings updateCompanyFIPSSessionSettings(
			long companyId, int idleTimeoutMinutes, int absoluteLifetimeMinutes)
		throws PortalException {

		return getService().updateCompanyFIPSSessionSettings(
			companyId, idleTimeoutMinutes, absoluteLifetimeMinutes);
	}

	public static FIPSSessionSettingsService getService() {
		return _serviceSnapshot.get();
	}

	private static final Snapshot<FIPSSessionSettingsService> _serviceSnapshot =
		new Snapshot<>(
			FIPSSessionSettingsServiceUtil.class,
			FIPSSessionSettingsService.class);

}
// LIFERAY-SERVICE-BUILDER-HASH:350567198