/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import java.util.Objects;

/**
 * @author Manuele Castro
 */
public enum CryptoFunction {

	DECAPSULATE("decapsulate"), DECRYPT("decrypt"), DIGEST("digest"),
	ENCAPSULATE("encapsulate"), ENCRYPT("encrypt"), GENERATE("generate"),
	KEYDERIVE("keyderive"), KEYGEN("keygen"), OTHER("other"), SIGN("sign"),
	TAG("tag"), UNKNOWN("unknown"), VERIFY("verify");

	public static CryptoFunction parse(String value) {
		for (CryptoFunction cryptoFunction : values()) {
			if (Objects.equals(cryptoFunction.getValue(), value)) {
				return cryptoFunction;
			}
		}

		throw new IllegalArgumentException("Invalid crypto function " + value);
	}

	public String getValue() {
		return _value;
	}

	private CryptoFunction(String value) {
		_value = value;
	}

	private final String _value;

}