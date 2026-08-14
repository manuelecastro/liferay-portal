/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * Scoped per company so that each portal instance carries its own timeouts.
 *
 * <p>
 * Declares no category and sets <code>generateUI</code> to false, which keeps
 * the configuration out of the System Settings listing and out of its search.
 * That is presentation only: the configuration admin edit, save, delete, and
 * export commands never consult <code>generateUI</code>, so hiding the entry
 * would not stop a company administrator who knows the PID. Authorization is
 * enforced on every write by {@link
 * com.liferay.portal.security.fips.internal.configuration.persistence.listener.FIPSSessionConfigurationModelListener}.
 * </p>
 *
 * @author Manuele Castro
 */
@ExtendedObjectClassDefinition(
	generateUI = false, scope = ExtendedObjectClassDefinition.Scope.COMPANY
)
@Meta.OCD(
	id = "com.liferay.portal.security.fips.configuration.FIPSSessionConfiguration",
	localization = "content/Language", name = "fips-session-configuration-name"
)
public interface FIPSSessionConfiguration {

	@Meta.AD(
		deflt = "43200", name = "session-absolute-lifetime", required = false
	)
	public int absoluteLifetimeMinutes();

	@Meta.AD(deflt = "15", name = "session-idle-timeout", required = false)
	public int idleTimeoutMinutes();

}