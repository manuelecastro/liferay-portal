/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.util.ListUtil;

import java.util.Collections;
import java.util.List;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;

/**
 * Describes one crypto call that the scan finds in a class's bytecode: the JCA
 * class it calls, and the algorithm or the settings that choose it, when the
 * bytecode shows them.
 *
 * @author Manuele Castro
 */
public class CryptoCall {

	public CryptoCall(
		String className, String featureName, String jcaClassName) {

		_className = className;
		_featureName = featureName;
		_jcaClassName = jcaClassName;
	}

	public String getAlgorithm() {
		return _algorithm;
	}

	public String getClassName() {
		return _className;
	}

	public List<String> getConfiguredBy() {
		return _configuredBy;
	}

	public List<CryptoFunction> getCryptoFunctions() {
		return _cryptoFunctions;
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

	public String getUnresolvedReason() {
		return _unresolvedReason;
	}

	public boolean isResolved() {
		if ((_algorithm != null) || ListUtil.isNotEmpty(_configuredBy)) {
			return true;
		}

		return false;
	}

	public void setAlgorithm(String algorithm) {
		_algorithm = algorithm;
	}

	public void setConfiguredBy(List<String> configuredBy) {
		_configuredBy = configuredBy;
	}

	public void setCryptoFunctions(List<CryptoFunction> cryptoFunctions) {
		_cryptoFunctions = cryptoFunctions;
	}

	public void setKeySize(int keySize) {
		_keySize = keySize;
	}

	public void setUnresolvedReason(String unresolvedReason) {
		_unresolvedReason = unresolvedReason;
	}

	private String _algorithm;
	private final String _className;
	private List<String> _configuredBy = Collections.emptyList();
	private List<CryptoFunction> _cryptoFunctions = Collections.emptyList();
	private final String _featureName;
	private final String _jcaClassName;
	private int _keySize;
	private String _unresolvedReason;

}