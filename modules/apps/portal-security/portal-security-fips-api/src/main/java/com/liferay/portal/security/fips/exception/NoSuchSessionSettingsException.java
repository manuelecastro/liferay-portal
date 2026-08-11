/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.exception;

import com.liferay.portal.kernel.exception.NoSuchModelException;

/**
 * @author Brian Wing Shun Chan
 */
public class NoSuchSessionSettingsException extends NoSuchModelException {

	public NoSuchSessionSettingsException() {
	}

	public NoSuchSessionSettingsException(String msg) {
		super(msg);
	}

	public NoSuchSessionSettingsException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public NoSuchSessionSettingsException(Throwable throwable) {
		super(throwable);
	}

}