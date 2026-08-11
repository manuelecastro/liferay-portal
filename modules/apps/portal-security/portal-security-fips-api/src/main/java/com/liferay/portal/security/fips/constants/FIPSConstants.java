/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.constants;

/**
 * @author Manuele Castro
 */
public class FIPSConstants {

	public static final String CRYPTO_OFFICER_PASSWORD_POLICY_NAME =
		"Crypto Officer Password Policy";

	public static final String FIPS_ADMIN_PORTLET_ID =
		"com_liferay_portal_security_fips_web_internal_portlet_" +
			"FIPSAdminPortlet";

	public static final String SESSION_ABSOLUTE_DEADLINE =
		"FIPS_SESSION_ABSOLUTE_DEADLINE";

	public static final int SESSION_ABSOLUTE_LIFETIME_DEFAULT_MINUTES = 43200;

	public static final int SESSION_ABSOLUTE_LIFETIME_MAX_MINUTES = 43200;

	public static final int SESSION_IDLE_TIMEOUT_DEFAULT_MINUTES = 15;

	public static final int SESSION_IDLE_TIMEOUT_MAX_MINUTES = 720;

}