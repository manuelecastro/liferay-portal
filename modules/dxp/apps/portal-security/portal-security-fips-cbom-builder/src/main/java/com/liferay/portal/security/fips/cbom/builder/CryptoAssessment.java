/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TreeMapBuilder;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.cyclonedx.model.component.crypto.enums.CertificationLevel;

/**
 * @author Manuele Castro
 */
public class CryptoAssessment {

	public static CryptoAssessment load(
			String fipsProviderName, String fipsProviderVersion)
		throws IOException {

		ObjectMapper objectMapper = new ObjectMapper();

		try (InputStream inputStream =
				CryptoAssessment.class.getResourceAsStream(
					"dependencies/cbom-reference-data.json")) {

			return new CryptoAssessment(
				fipsProviderName, fipsProviderVersion,
				objectMapper.readTree(inputStream));
		}
	}

	public CryptoAssessment(
		String fipsProviderName, String fipsProviderVersion,
		JsonNode referenceDataJsonNode) {

		_referenceDataJsonNode = referenceDataJsonNode;

		_moduleJsonNode = _fetchModuleJsonNode(
			fipsProviderName, fipsProviderVersion);
	}

	public String fetchCertificateURL() {
		if (_moduleJsonNode == null) {
			return null;
		}

		JsonNode certificateJsonNode = _moduleJsonNode.get("certificate");

		String source = _getText(certificateJsonNode, "source");

		return source.substring(0, source.indexOf(CharPool.COMMA));
	}

	public Integer fetchNISTQuantumSecurityLevel(String name) {
		for (JsonNode jsonNode :
				_referenceDataJsonNode.get("quantumSecurityLevels")) {

			for (JsonNode familyJsonNode : jsonNode.get("families")) {
				if (_isFamily(familyJsonNode.asText(), name)) {
					return _getInt(jsonNode, "level");
				}
			}
		}

		return null;
	}

	public CertificationLevel getCertificationLevel(String fipsApprovalStatus) {
		if ((_moduleJsonNode == null) ||
			(!Objects.equals(fipsApprovalStatus, "APPROVED") &&
			 !Objects.equals(fipsApprovalStatus, "VENDOR_AFFIRMED"))) {

			return CertificationLevel.NONE;
		}

		JsonNode certificateJsonNode = _moduleJsonNode.get("certificate");

		return CertificationLevel.fromString(
			_getText(certificateJsonNode, "level"));
	}

	public String getCurveStatus(String curve) {
		if (curve == null) {
			return "unknown";
		}

		for (JsonNode jsonNode : _referenceDataJsonNode.get("curves")) {
			if (!_contains(jsonNode.get("names"), curve)) {
				continue;
			}

			return _getText(jsonNode, "status");
		}

		return "not-listed";
	}

	public String getFIPSApprovalStatus(
		String jcaClassName, String name, String providerName) {

		if ((_moduleJsonNode == null) ||
			((providerName == null) &&
			 !Objects.equals(jcaClassName, "SecretKeySpec")) ||
			((providerName != null) && !isFIPSModuleProvider(providerName))) {

			return "PROVIDER_NOT_VALIDATED";
		}

		JsonNode securityPolicyJsonNode = _moduleJsonNode.get("securityPolicy");

		for (Map.Entry<String, String> entry : _categories.entrySet()) {
			for (JsonNode jsonNode :
					securityPolicyJsonNode.get(entry.getValue())) {

				if (_matches(jsonNode.asText(), jcaClassName, name)) {
					return entry.getKey();
				}
			}
		}

		return "UNKNOWN";
	}

	public Map<String, String> getModuleProperties() {
		if (_moduleJsonNode == null) {
			return Collections.emptyMap();
		}

		JsonNode certificateJsonNode = _moduleJsonNode.get("certificate");

		return TreeMapBuilder.put(
			"liferay:cbom:cmvp-certificate",
			_getText(certificateJsonNode, "number")
		).put(
			"liferay:cbom:cmvp-status", _getText(certificateJsonNode, "status")
		).put(
			"liferay:cbom:cmvp-sunset-date",
			_getText(certificateJsonNode, "sunsetDate")
		).put(
			"liferay:cbom:fips-module", _getText(_moduleJsonNode, "name")
		).build();
	}

	public String getPQCReadiness() {
		JsonNode jsonNode = _referenceDataJsonNode.get("pqcReadiness");

		return _getText(jsonNode, "value");
	}

	public String getRandomNumberGenerationNote() {
		if (_moduleJsonNode == null) {
			return StringPool.BLANK;
		}

		JsonNode rngJsonNode = _moduleJsonNode.get("rng");

		return _getText(rngJsonNode, "entropy") + StringPool.SPACE +
			_getText(rngJsonNode, "healthTests");
	}

	public List<String> getVendorAffirmedMethods() {
		if (_moduleJsonNode == null) {
			return Collections.emptyList();
		}

		JsonNode securityPolicyJsonNode = _moduleJsonNode.get("securityPolicy");

		List<String> vendorAffirmedMethods = new ArrayList<>();

		for (JsonNode jsonNode :
				securityPolicyJsonNode.get("vendorAffirmedMethods")) {

			vendorAffirmedMethods.add(jsonNode.asText());
		}

		return vendorAffirmedMethods;
	}

	public boolean hasFIPSModule() {
		if (_moduleJsonNode == null) {
			return false;
		}

		return true;
	}

	public boolean isFIPSModuleProvider(String providerName) {
		if ((_moduleJsonNode == null) ||
			!Objects.equals(
				providerName, _getText(_moduleJsonNode, "providerName"))) {

			return false;
		}

		return true;
	}

	public boolean isNISTApprovedCurve(String curve, String use) {
		for (JsonNode jsonNode : _referenceDataJsonNode.get("curves")) {
			if (_contains(jsonNode.get("names"), curve)) {
				return _contains(jsonNode.get("uses"), use);
			}
		}

		return false;
	}

	private boolean _contains(JsonNode jsonNode, String value) {
		for (JsonNode elementJsonNode : jsonNode) {
			if (Objects.equals(elementJsonNode.asText(), value)) {
				return true;
			}
		}

		return false;
	}

	private JsonNode _fetchModuleJsonNode(
		String fipsProviderName, String fipsProviderVersion) {

		if ((fipsProviderName == null) || (fipsProviderVersion == null)) {
			return null;
		}

		for (JsonNode jsonNode : _referenceDataJsonNode.get("modules")) {
			if (Objects.equals(
					_getText(jsonNode, "providerName"), fipsProviderName) &&
				_contains(jsonNode.get("versions"), fipsProviderVersion)) {

				return jsonNode;
			}
		}

		return null;
	}

	private int _getInt(JsonNode jsonNode, String fieldName) {
		JsonNode fieldJsonNode = jsonNode.get(fieldName);

		return fieldJsonNode.asInt();
	}

	private String _getText(JsonNode jsonNode, String fieldName) {
		JsonNode fieldJsonNode = jsonNode.get(fieldName);

		return fieldJsonNode.asText();
	}

	private boolean _isFamily(String family, String name) {
		if (Objects.equals(family, StringPool.STAR) ||
			Objects.equals(family, name) ||
			name.startsWith(family + StringPool.DASH)) {

			return true;
		}

		return false;
	}

	private boolean _matches(String token, String jcaClassName, String name) {
		int index = token.indexOf(CharPool.COLON);

		String tokenJCAClassName = token.substring(0, index);

		if (!Objects.equals(tokenJCAClassName, StringPool.STAR) &&
			!StringUtil.equals(tokenJCAClassName, jcaClassName)) {

			return false;
		}

		return _isFamily(token.substring(index + 1), name);
	}

	private static final Map<String, String> _categories =
		LinkedHashMapBuilder.put(
			"NON_APPROVED_NOT_ALLOWED", "nonApprovedNotAllowed"
		).put(
			"VENDOR_AFFIRMED", "vendorAffirmed"
		).put(
			"APPROVED", "approved"
		).put(
			"NON_APPROVED_ALLOWED", "nonApprovedAllowed"
		).put(
			"NON_APPROVED_ALLOWED_NO_SECURITY_CLAIMED",
			"nonApprovedAllowedNoSecurityClaimed"
		).build();

	private final JsonNode _moduleJsonNode;
	private final JsonNode _referenceDataJsonNode;

}