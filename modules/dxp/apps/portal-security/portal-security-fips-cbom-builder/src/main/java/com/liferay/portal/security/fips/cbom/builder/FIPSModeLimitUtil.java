/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.portal.kernel.security.fips.FIPSModeValidator;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * @author Manuele Castro
 */
public class FIPSModeLimitUtil {

	public static List<String> getJWSAlgorithms() {
		return _getSortedValues("_allowedJWSAlgorithms");
	}

	private static <T> T _getFieldValue(String name) {
		try {
			Field field = ReflectionUtil.getDeclaredField(
				FIPSModeValidator.class, name);

			return (T)field.get(null);
		}
		catch (Exception exception) {
			throw new IllegalStateException(
				"Unable to read FIPSModeValidator's " + name, exception);
		}
	}

	private static <T> List<T> _getSortedValues(String name) {
		Collection<T> values = _getFieldValue(name);

		return new ArrayList<>(new TreeSet<>(values));
	}

}