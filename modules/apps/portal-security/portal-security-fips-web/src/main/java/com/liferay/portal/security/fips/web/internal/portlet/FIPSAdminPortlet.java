/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.web.internal.portlet;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.security.fips.web.internal.constants.FIPSPortletKeys;

import jakarta.portlet.Portlet;

import org.osgi.service.component.annotations.Component;

/**
 * @author Manuele Castro
 */
@Component(
	property = {
		"com.liferay.portlet.css-class-wrapper=portal-security-fips-portlet",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.instanceable=false",
		"jakarta.portlet.display-name=FIPS Admin",
		"jakarta.portlet.expiration-cache=0",
		"jakarta.portlet.info.keywords=FIPS Admin",
		"jakarta.portlet.info.short-title=FIPS Admin",
		"jakarta.portlet.info.title=FIPS Admin",
		"jakarta.portlet.init-param.clear-request-parameters=true",
		"jakarta.portlet.init-param.copy-request-parameters=true",
		"jakarta.portlet.init-param.view-template=/view.jsp",
		"jakarta.portlet.name=" + FIPSPortletKeys.FIPS_ADMIN,
		"jakarta.portlet.resource-bundle=content.Language",
		"jakarta.portlet.version=4.0"
	},
	service = Portlet.class
)
public class FIPSAdminPortlet extends MVCPortlet {
}