/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.util.ListUtil;

import java.util.Collections;
import java.util.List;

/**
 * Describes one way a feature uses cryptography: the JCA class the feature's
 * classes call, and either the algorithm the release fixes or the portal
 * properties that choose it.
 *
 * @author Manuele Castro
 */
public class CryptoUsage {

	public CryptoUsage(
		List<String> classNames, List<CryptoFunction> cryptoFunctions,
		CryptoPrimitive cryptoPrimitive, String featureName,
		String jcaClassName) {

		_classNames = classNames;
		_cryptoFunctions = cryptoFunctions;
		_cryptoPrimitive = cryptoPrimitive;
		_featureName = featureName;
		_jcaClassName = jcaClassName;
	}

	public String getAlgorithm() {
		return _algorithm;
	}

	public List<String> getClassNames() {
		return _classNames;
	}

	public List<String> getConfiguredBy() {
		return _configuredBy;
	}

	public List<CryptoFunction> getCryptoFunctions() {
		return _cryptoFunctions;
	}

	public CryptoPrimitive getCryptoPrimitive() {
		return _cryptoPrimitive;
	}

	public String getCurve() {
		return _curve;
	}

	public String getFeatureName() {
		return _featureName;
	}

	public String getJCAClassName() {
		return _jcaClassName;
	}

	public int getKeySize() {
		return _keySize;
	}

	public boolean isConfigurable() {
		return ListUtil.isNotEmpty(_configuredBy);
	}

	public void setAlgorithm(String algorithm) {
		_algorithm = algorithm;
	}

	public void setConfiguredBy(List<String> configuredBy) {
		_configuredBy = configuredBy;
	}

	public void setCurve(String curve) {
		_curve = curve;
	}

	public void setKeySize(int keySize) {
		_keySize = keySize;
	}

	private String _algorithm;
	private final List<String> _classNames;
	private List<String> _configuredBy = Collections.emptyList();
	private final List<CryptoFunction> _cryptoFunctions;
	private final CryptoPrimitive _cryptoPrimitive;
	private String _curve;
	private final String _featureName;
	private final String _jcaClassName;
	private int _keySize;

}