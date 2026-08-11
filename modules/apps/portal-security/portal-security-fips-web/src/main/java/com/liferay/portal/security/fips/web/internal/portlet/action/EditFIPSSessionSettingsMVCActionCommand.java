/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.web.internal.portlet.action;

import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.security.fips.service.FIPSSessionSettingsService;
import com.liferay.portal.security.fips.web.internal.constants.FIPSPortletKeys;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Calls the remote service rather than the local one, so that the Crypto
 * Officer check applies to the save.
 *
 * @author Manuele Castro
 */
@Component(
	property = {
		"jakarta.portlet.name=" + FIPSPortletKeys.FIPS_ADMIN,
		"mvc.command.name=/fips_admin/edit_fips_session_settings"
	},
	service = MVCActionCommand.class
)
public class EditFIPSSessionSettingsMVCActionCommand
	extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		_fipsSessionSettingsService.updateCompanyFIPSSessionSettings(
			themeDisplay.getCompanyId(),
			ParamUtil.getInteger(actionRequest, "idleTimeoutMinutes"),
			ParamUtil.getInteger(actionRequest, "absoluteLifetimeMinutes"));
	}

	@Reference
	private FIPSSessionSettingsService _fipsSessionSettingsService;

}