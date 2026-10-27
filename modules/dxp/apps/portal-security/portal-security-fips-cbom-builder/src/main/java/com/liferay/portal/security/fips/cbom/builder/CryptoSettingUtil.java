/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;

/**
 * @author Manuele Castro
 */
public class CryptoSettingUtil {

	public static String getFileName(String settingKey) {
		int index = settingKey.indexOf(CharPool.AT);

		if (index != -1) {
			return settingKey.substring(index + 1);
		}

		if (CryptoCallScanner.isJDKPropertyKey(settingKey)) {
			return "java.security";
		}

		return "portal.properties";
	}

	public static String getName(String settingKey) {
		int index = settingKey.indexOf(CharPool.AT);

		if (index == -1) {
			return settingKey;
		}

		return settingKey.substring(0, index);
	}

	public static String toSettingKey(String fileName, String name) {
		return name + StringPool.AT + fileName;
	}

}