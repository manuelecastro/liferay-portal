/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.impl;

import com.liferay.portal.aop.AopService;
import com.liferay.portal.security.fips.service.base.FIPSSessionSettingsLocalServiceBaseImpl;

import org.osgi.service.component.annotations.Component;

/**
 * @author Brian Wing Shun Chan
 */
@Component(
	property = "model.class.name=com.liferay.portal.security.fips.model.FIPSSessionSettings",
	service = AopService.class
)
public class FIPSSessionSettingsLocalServiceImpl
	extends FIPSSessionSettingsLocalServiceBaseImpl {
}