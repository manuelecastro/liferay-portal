/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.util;

import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.security.fips.constants.FIPSConstants;

import java.util.concurrent.TimeUnit;

/**
 * Converts a timeout that the Crypto Officer entered in minutes, hours, or
 * days into the minutes that the ceilings and the enforcement components work
 * in.
 *
 * @author Manuele Castro
 */
public class FIPSTimeUnitUtil {

	/**
	 * Returns the largest ceiling expressible in the time unit, so that the
	 * page can cap its input without restating the limit per unit.
	 */
	public static int getMaximum(int maximumMinutes, String timeUnit) {
		if (StringUtil.equals(timeUnit, FIPSConstants.TIME_UNIT_DAYS)) {
			return (int)TimeUnit.MINUTES.toDays(maximumMinutes);
		}

		if (StringUtil.equals(timeUnit, FIPSConstants.TIME_UNIT_HOURS)) {
			return (int)TimeUnit.MINUTES.toHours(maximumMinutes);
		}

		return maximumMinutes;
	}

	/**
	 * Returns the value in minutes. An unrecognized unit is read as minutes,
	 * which is the most restrictive reading and therefore the safe one when a
	 * stored value predates a unit or was written by hand.
	 */
	public static long toMinutes(int value, String timeUnit) {
		if (StringUtil.equals(timeUnit, FIPSConstants.TIME_UNIT_DAYS)) {
			return TimeUnit.DAYS.toMinutes(value);
		}

		if (StringUtil.equals(timeUnit, FIPSConstants.TIME_UNIT_HOURS)) {
			return TimeUnit.HOURS.toMinutes(value);
		}

		return value;
	}

}