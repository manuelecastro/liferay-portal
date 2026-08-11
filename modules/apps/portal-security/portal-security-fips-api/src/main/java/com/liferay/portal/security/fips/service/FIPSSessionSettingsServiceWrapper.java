/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service;

import com.liferay.portal.kernel.service.ServiceWrapper;

/**
 * Provides a wrapper for {@link FIPSSessionSettingsService}.
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettingsService
 * @generated
 */
public class FIPSSessionSettingsServiceWrapper
	implements FIPSSessionSettingsService,
			   ServiceWrapper<FIPSSessionSettingsService> {

	public FIPSSessionSettingsServiceWrapper() {
		this(null);
	}

	public FIPSSessionSettingsServiceWrapper(
		FIPSSessionSettingsService fipsSessionSettingsService) {

		_fipsSessionSettingsService = fipsSessionSettingsService;
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return _fipsSessionSettingsService.getOSGiServiceIdentifier();
	}

	@Override
	public FIPSSessionSettingsService getWrappedService() {
		return _fipsSessionSettingsService;
	}

	@Override
	public void setWrappedService(
		FIPSSessionSettingsService fipsSessionSettingsService) {

		_fipsSessionSettingsService = fipsSessionSettingsService;
	}

	private FIPSSessionSettingsService _fipsSessionSettingsService;

}
// LIFERAY-SERVICE-BUILDER-HASH:1286036433