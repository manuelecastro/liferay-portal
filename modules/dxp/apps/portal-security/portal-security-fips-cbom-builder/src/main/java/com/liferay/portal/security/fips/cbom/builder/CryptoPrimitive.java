/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import java.util.Objects;

/**
 * @author Manuele Castro
 */
public enum CryptoPrimitive {

	AE("ae"), BLOCK_CIPHER("block-cipher"), COMBINER("combiner"), DRBG("drbg"),
	HASH("hash"), KDF("kdf"), KEM("kem"), KEY_AGREE("key-agree"), MAC("mac"),
	OTHER("other"), PKE("pke"), SIGNATURE("signature"),
	STREAM_CIPHER("stream-cipher"), UNKNOWN("unknown"), XOF("xof");

	public static CryptoPrimitive parse(String value) {
		for (CryptoPrimitive cryptoPrimitive : values()) {
			if (Objects.equals(cryptoPrimitive.getValue(), value)) {
				return cryptoPrimitive;
			}
		}

		throw new IllegalArgumentException("Invalid crypto primitive " + value);
	}

	public String getValue() {
		return _value;
	}

	private CryptoPrimitive(String value) {
		_value = value;
	}

	private final String _value;

}