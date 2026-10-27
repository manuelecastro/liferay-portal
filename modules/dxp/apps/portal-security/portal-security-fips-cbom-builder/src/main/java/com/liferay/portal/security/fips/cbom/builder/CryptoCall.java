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
 * @author Manuele Castro
 */
public class CryptoCall {

	public CryptoCall(
		String bundleSymbolicName, String className, String featureName,
		String jcaClassName) {

		_bundleSymbolicName = bundleSymbolicName;
		_className = className;
		_featureName = featureName;
		_jcaClassName = jcaClassName;
	}

	public CryptoCall copy() {
		CryptoCall cryptoCall = new CryptoCall(
			_bundleSymbolicName, _className, _featureName, _jcaClassName);

		cryptoCall.setAlgorithm(_algorithm);
		cryptoCall.setConfiguredBy(_configuredBy);
		cryptoCall.setCryptoFunctions(_cryptoFunctions);
		cryptoCall.setCurve(_curve);
		cryptoCall.setKeySize(_keySize);
		cryptoCall.setLibrary(_library);
		cryptoCall.setNote(_note);
		cryptoCall.setRelatedCryptoMaterialTypes(_relatedCryptoMaterialTypes);
		cryptoCall.setUnresolvedReason(_unresolvedReason);

		return cryptoCall;
	}

	public String getAlgorithm() {
		return _algorithm;
	}

	public String getBundleSymbolicName() {
		return _bundleSymbolicName;
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

	public String getLibrary() {
		return _library;
	}

	public String getNote() {
		return _note;
	}

	public Primitive getPrimitive() {
		return CryptoPrimitiveUtil.getPrimitive(_algorithm, _jcaClassName);
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

	public void setCryptoFunctions(List<CryptoFunction> cryptoFunctions) {
		_cryptoFunctions = cryptoFunctions;
	}

	public void setCurve(String curve) {
		_curve = curve;
	}

	public void setKeySize(int keySize) {
		_keySize = keySize;
	}

	public void setLibrary(String library) {
		_library = library;
	}

	public void setNote(String note) {
		_note = note;
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
	private final String _className;
	private List<String> _configuredBy = Collections.emptyList();
	private List<CryptoFunction> _cryptoFunctions = Collections.emptyList();
	private String _curve;
	private final String _featureName;
	private final String _jcaClassName;
	private int _keySize;
	private String _library;
	private String _note;
	private List<RelatedCryptoMaterialType> _relatedCryptoMaterialTypes =
		Collections.emptyList();
	private String _unresolvedReason;

}