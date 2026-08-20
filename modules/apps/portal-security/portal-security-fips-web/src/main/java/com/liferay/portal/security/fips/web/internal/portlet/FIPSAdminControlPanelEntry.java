/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.web.internal.portlet;

import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.portlet.BaseControlPanelEntry;
import com.liferay.portal.kernel.portlet.ControlPanelEntry;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.security.fips.constants.FIPSPortletKeys;
import com.liferay.portal.security.fips.util.FIPSUtil;

import org.osgi.service.component.annotations.Component;

/**
 * Hides the page from everyone but the Crypto Officer.
 *
 * <p>
 * The denial is expressed through {@link #hasAccessPermissionDenied}, which
 * {@link BaseControlPanelEntry} evaluates before any explicit or implicit
 * grant. A company administrator and an omniadmin pass every resource
 * permission lookup, so a grant-based check alone would let them in.
 * </p>
 *
 * @author Manuele Castro
 */
@Component(
	property = "jakarta.portlet.name=" + FIPSPortletKeys.FIPS_ADMIN,
	service = ControlPanelEntry.class
)
public class FIPSAdminControlPanelEntry extends BaseControlPanelEntry {

	@Override
	protected boolean hasAccessPermissionDenied(
			PermissionChecker permissionChecker, Group group, Portlet portlet)
		throws Exception {

		if (!FIPSUtil.hasCryptoOfficerRole(permissionChecker.getUser())) {
			return true;
		}

		return super.hasAccessPermissionDenied(
			permissionChecker, group, portlet);
	}

}