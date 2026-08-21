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
 * Each timeout is stored as an amount plus the unit it was entered in, rather
 * than normalized to minutes, so the page can show the Crypto Officer the same
 * figure they typed. Everything that enforces a timeout converts through
 * {@link com.liferay.portal.security.fips.util.FIPSTimeUnitUtil} first.
 * </p>
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
		deflt = "30", name = "fips-session-absolute-lifetime", required = false
	)
	public int absoluteLifetime();

	@Meta.AD(
		deflt = "days", name = "time-unit",
		optionLabels = {"minutes", "hours", "days"},
		optionValues = {"minutes", "hours", "days"}, required = false
	)
	public String absoluteLifetimeTimeUnit();

	@Meta.AD(deflt = "15", name = "fips-session-idle-timeout", required = false)
	public int idleTimeout();

	@Meta.AD(
		deflt = "minutes", name = "time-unit",
		optionLabels = {"minutes", "hours"},
		optionValues = {"minutes", "hours"}, required = false
	)
	public String idleTimeoutTimeUnit();

}