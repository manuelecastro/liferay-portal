/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Manuele Castro
 * @author Caio Farias
 */
public class CBOMData {

	public void addCryptoUse(CryptoUse cryptoUse) {
		_scannedCryptoUses.addAll(_toCryptoUses(cryptoUse));
	}

	public List<CryptoUse> getScannedCryptoUses() {
		return _scannedCryptoUses;
	}

	private List<CryptoUse> _toCryptoUses(CryptoUse cryptoUse) {
		List<CryptoUse> cryptoUses = new ArrayList<>();

		for (String value : cryptoUse.getValues()) {
			CryptoUse valueCryptoUse = cryptoUse.createPart();

			valueCryptoUse.setAlgorithm(value);

			cryptoUses.add(valueCryptoUse);
		}

		return cryptoUses;
	}

	private final List<CryptoUse> _scannedCryptoUses = new ArrayList<>();

}