/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.impl;

import com.liferay.portal.aop.AopService;
import com.liferay.portal.security.fips.service.base.FIPSSessionSettingsServiceBaseImpl;

import org.osgi.service.component.annotations.Component;

/**
 * @author Brian Wing Shun Chan
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
}