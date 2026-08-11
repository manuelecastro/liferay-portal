/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.model;

import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.sql.dsl.base.BaseTable;

import java.sql.Types;

import java.util.Date;

/**
 * The table class for the &quot;FIPSSessionSettings&quot; database table.
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettings
 * @generated
 */
public class FIPSSessionSettingsTable
	extends BaseTable<FIPSSessionSettingsTable> {

	public static final FIPSSessionSettingsTable INSTANCE =
		new FIPSSessionSettingsTable();

	public final Column<FIPSSessionSettingsTable, Long> mvccVersion =
		createColumn(
			"mvccVersion", Long.class, Types.BIGINT, Column.FLAG_NULLITY);
	public final Column<FIPSSessionSettingsTable, Long> fipsSessionSettingsId =
		createColumn(
			"fipsSessionSettingsId", Long.class, Types.BIGINT,
			Column.FLAG_PRIMARY);
	public final Column<FIPSSessionSettingsTable, Long> companyId =
		createColumn(
			"companyId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<FIPSSessionSettingsTable, Long> userId = createColumn(
		"userId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<FIPSSessionSettingsTable, String> userName =
		createColumn(
			"userName", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<FIPSSessionSettingsTable, Date> createDate =
		createColumn(
			"createDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<FIPSSessionSettingsTable, Date> modifiedDate =
		createColumn(
			"modifiedDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<FIPSSessionSettingsTable, Integer>
		absoluteLifetimeMinutes = createColumn(
			"absoluteLifetimeMinutes", Integer.class, Types.INTEGER,
			Column.FLAG_DEFAULT);
	public final Column<FIPSSessionSettingsTable, Integer> idleTimeoutMinutes =
		createColumn(
			"idleTimeoutMinutes", Integer.class, Types.INTEGER,
			Column.FLAG_DEFAULT);

	private FIPSSessionSettingsTable() {
		super("FIPSSessionSettings", FIPSSessionSettingsTable::new);
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:-1037271138