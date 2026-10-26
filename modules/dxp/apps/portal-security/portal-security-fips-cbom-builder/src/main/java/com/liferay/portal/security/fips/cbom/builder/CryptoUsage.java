/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.util.ListUtil;

import java.util.Collections;
import java.util.List;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;
import org.cyclonedx.model.component.crypto.enums.RelatedCryptoMaterialType;

/**
 * Describes one way a module uses cryptography: the JCA class the module's
 * classes call, and either the algorithm the release fixes, the settings that
 * choose it, or, when the scan cannot read either, where the algorithm comes
 * from.
 *
 * @author Manuele Castro
 */
public class CryptoUsage {

	public CryptoUsage(
		String bundleSymbolicName, List<String> classNames,
		List<CryptoFunction> cryptoFunctions, String featureName,
		String jcaClassName, Primitive primitive) {

		_bundleSymbolicName = bundleSymbolicName;
		_classNames = classNames;
		_cryptoFunctions = cryptoFunctions;
		_featureName = featureName;
		_jcaClassName = jcaClassName;
		_primitive = primitive;
	}

	public String getAlgorithm() {
		return _algorithm;
	}

	public String getBundleSymbolicName() {
		return _bundleSymbolicName;
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

	public Primitive getPrimitive() {
		return _primitive;
	}

	public List<RelatedCryptoMaterialType> getRelatedCryptoMaterialTypes() {
		return _relatedCryptoMaterialTypes;
	}

	public String getUnresolvedReason() {
		return _unresolvedReason;
	}

	public boolean isConfigurable() {
		return ListUtil.isNotEmpty(_configuredBy);
	}

	public boolean isResolved() {
		if ((_algorithm != null) || isConfigurable()) {
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

	public void setCurve(String curve) {
		_curve = curve;
	}

	public void setKeySize(int keySize) {
		_keySize = keySize;
	}

	public void setRelatedCryptoMaterialTypes(
		List<RelatedCryptoMaterialType> relatedCryptoMaterialTypes) {

		_relatedCryptoMaterialTypes = relatedCryptoMaterialTypes;
	}

	public void setUnresolvedReason(String unresolvedReason) {
		_unresolvedReason = unresolvedReason;
	}

	private String _algorithm;
	private final String _bundleSymbolicName;
	private final List<String> _classNames;
	private List<String> _configuredBy = Collections.emptyList();
	private final List<CryptoFunction> _cryptoFunctions;
	private String _curve;
	private final String _featureName;
	private final String _jcaClassName;
	private int _keySize;
	private final Primitive _primitive;
	private List<RelatedCryptoMaterialType> _relatedCryptoMaterialTypes =
		Collections.emptyList();
	private String _unresolvedReason;

}