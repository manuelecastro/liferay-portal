/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.File;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Manuele Castro
 */
public class CBOMReviewer {

	public static Set<String> getCertificates(JsonNode jsonNode) {
		Set<String> certificates = new TreeSet<>();

		for (JsonNode componentJsonNode : jsonNode.get("components")) {
			Map<String, String> properties = _getProperties(componentJsonNode);

			String certificate = properties.get(
				"liferay:cbom:cmvp-certificate");

			if ((certificate == null) ||
				!Objects.equals(
					_getText(componentJsonNode, "type"), "library")) {

				continue;
			}

			certificates.add(
				StringBundler.concat(
					_getText(componentJsonNode, "name"), " ",
					_getText(componentJsonNode, "version"),
					": CMVP certificate ", certificate, ", ",
					properties.get("liferay:cbom:cmvp-status"),
					", sunset date ",
					properties.get("liferay:cbom:cmvp-sunset-date"), ", ",
					_getCertificationReportURL(componentJsonNode)));
		}

		return certificates;
	}

	public static void main(String[] args) throws Exception {
		if ((args.length < 2) || Validator.isNull(args[1])) {
			throw new IllegalArgumentException(
				"Set \"cbom.previous.dir\" to the directory that holds the " +
					"previous release's CBOMs");
		}

		File cbomDir = new File(args[0]);
		File previousCBOMDir = new File(args[1]);

		File[] files = cbomDir.listFiles();

		if (files == null) {
			throw new IllegalArgumentException(
				"Directory \"" + cbomDir + "\" does not exist");
		}

		Arrays.sort(files);

		ObjectMapper objectMapper = new ObjectMapper();

		for (File file : files) {
			String fileName = file.getName();

			if (!fileName.endsWith(_EXTENSION)) {
				continue;
			}

			String configurationName = StringUtil.removeSubstring(
				fileName, _EXTENSION);
			JsonNode jsonNode = objectMapper.readTree(file);

			File previousFile = _fetchPreviousFile(
				configurationName, previousCBOMDir);

			if (previousFile == null) {
				System.out.println(
					StringBundler.concat(
						"The CBOM of ", configurationName,
						" has no previous version in ", previousCBOMDir,
						". Review the whole file before publishing it."));
			}
			else {
				Map<String, Set<String>> changes = review(
					jsonNode, objectMapper.readTree(previousFile));

				if (changes.isEmpty()) {
					System.out.println(
						StringBundler.concat(
							"The CBOM of ", configurationName,
							" has not changed since ", previousFile,
							". Publish it without a review."));
				}
				else {
					System.out.println(
						StringBundler.concat(
							"The CBOM of ", configurationName,
							" has changed since ", previousFile,
							". Review these changes before publishing it."));

					for (Map.Entry<String, Set<String>> entry :
							changes.entrySet()) {

						System.out.println();
						System.out.println(entry.getKey() + ":");

						for (String change : entry.getValue()) {
							System.out.println("- " + change);
						}
					}
				}
			}

			Set<String> certificates = getCertificates(jsonNode);

			if (!certificates.isEmpty()) {
				System.out.println();
				System.out.println(
					"Check that each CMVP certificate is still active and " +
						"not past its sunset date:");

				for (String certificate : certificates) {
					System.out.println("- " + certificate);
				}
			}

			System.out.println();
		}
	}

	public static Map<String, Set<String>> review(
		JsonNode jsonNode, JsonNode previousJsonNode) {

		Map<String, Set<String>> changes = new LinkedHashMap<>();

		for (String category : _categories) {
			changes.put(category, new TreeSet<>());
		}

		if (!Objects.equals(
				_normalize(jsonNode), _normalize(previousJsonNode))) {

			_reviewComponents(changes, jsonNode, previousJsonNode);
			_reviewMetadata(changes, jsonNode, previousJsonNode);
			_reviewUses(changes, jsonNode, previousJsonNode);

			if (_isEmpty(changes)) {
				_add(
					changes, _CATEGORY_CHANGED_ENTRIES,
					"The files differ in a way this review does not " +
						"classify. Compare them by hand.");
			}
		}

		Collection<Set<String>> values = changes.values();

		values.removeIf(Set::isEmpty);

		return changes;
	}

	private static void _add(
		Map<String, Set<String>> changes, String category, String change) {

		Set<String> categoryChanges = changes.get(category);

		categoryChanges.add(change);
	}

	private static File _fetchPreviousFile(
		String configurationName, File previousCBOMDir) {

		File[] files = previousCBOMDir.listFiles();

		if (files == null) {
			return null;
		}

		for (File file : files) {
			String fileName = file.getName();

			if (fileName.equals(configurationName + _EXTENSION) ||
				fileName.endsWith(
					StringBundler.concat(
						"-cbom-", configurationName, _EXTENSION))) {

				return file;
			}
		}

		return null;
	}

	private static String _getCertificationReportURL(JsonNode jsonNode) {
		JsonNode externalReferencesJsonNode = jsonNode.get(
			"externalReferences");

		if (externalReferencesJsonNode == null) {
			return null;
		}

		for (JsonNode externalReferenceJsonNode : externalReferencesJsonNode) {
			if (Objects.equals(
					_getText(externalReferenceJsonNode, "type"),
					"certification-report")) {

				return _getText(externalReferenceJsonNode, "url");
			}
		}

		return null;
	}

	private static String _getChange(
		String label, String previousValue, String value) {

		return StringBundler.concat(
			label, ": from ", GetterUtil.getString(previousValue, "none"),
			" to ", GetterUtil.getString(value, "none"));
	}

	private static Set<String> _getChangedFieldNames(
		JsonNode jsonNode, JsonNode previousJsonNode) {

		Set<String> changedFieldNames = new TreeSet<>();

		Set<String> fieldNames = SetUtil.fromIterator(jsonNode.fieldNames());

		fieldNames.addAll(SetUtil.fromIterator(previousJsonNode.fieldNames()));

		for (String fieldName : fieldNames) {
			if (Objects.equals(
					jsonNode.get(fieldName), previousJsonNode.get(fieldName))) {

				continue;
			}

			if (!fieldName.equals("properties")) {
				changedFieldNames.add(fieldName);

				continue;
			}

			changedFieldNames.add(
				StringBundler.concat(
					"properties (",
					StringUtil.merge(
						_getChangedPropertyNames(
							_getProperties(jsonNode),
							_getProperties(previousJsonNode)),
						", "),
					")"));
		}

		return changedFieldNames;
	}

	private static Set<String> _getChangedPropertyNames(
		Map<String, String> properties,
		Map<String, String> previousProperties) {

		Set<String> propertyNames = new TreeSet<>(properties.keySet());

		propertyNames.addAll(previousProperties.keySet());

		propertyNames.removeIf(
			propertyName -> Objects.equals(
				properties.get(propertyName),
				previousProperties.get(propertyName)));

		return propertyNames;
	}

	private static Map<String, JsonNode> _getComponentJsonNodes(
		JsonNode jsonNode) {

		Map<String, JsonNode> componentJsonNodes = new TreeMap<>();

		for (JsonNode componentJsonNode : jsonNode.get("components")) {
			componentJsonNodes.put(
				_getText(componentJsonNode, "bom-ref"), componentJsonNode);
		}

		return componentJsonNodes;
	}

	private static String _getFIPSApprovalStatus(JsonNode jsonNode) {
		Map<String, String> properties = _getProperties(jsonNode);

		return properties.get("liferay:cbom:fips-approval-status");
	}

	private static String _getLabel(String bomRef, JsonNode jsonNode) {
		return StringBundler.concat(
			_getText(jsonNode, "name"), " (", bomRef, ")");
	}

	private static Map<String, String> _getProperties(JsonNode jsonNode) {
		Map<String, String> properties = new TreeMap<>();

		JsonNode propertiesJsonNode = jsonNode.get("properties");

		if (propertiesJsonNode == null) {
			return properties;
		}

		for (JsonNode propertyJsonNode : propertiesJsonNode) {
			properties.put(
				_getText(propertyJsonNode, "name"),
				_getText(propertyJsonNode, "value"));
		}

		return properties;
	}

	private static JsonNode _getProtocolPropertiesJsonNode(JsonNode jsonNode) {
		JsonNode cryptoPropertiesJsonNode = jsonNode.get("cryptoProperties");

		if ((cryptoPropertiesJsonNode == null) ||
			!Objects.equals(
				_getText(cryptoPropertiesJsonNode, "assetType"), "protocol")) {

			return null;
		}

		return cryptoPropertiesJsonNode.get("protocolProperties");
	}

	private static Map<String, String> _getProviderNames(JsonNode jsonNode) {
		Map<String, String> providerNames = new TreeMap<>();

		for (Map.Entry<String, Set<String>> entry :
				_getRefs(
					jsonNode, "provides"
				).entrySet()) {

			for (String bomRef : entry.getValue()) {
				providerNames.put(
					bomRef,
					StringUtil.removeSubstring(entry.getKey(), "provider:"));
			}
		}

		return providerNames;
	}

	private static Map<String, Set<String>> _getRefs(
		JsonNode jsonNode, String fieldName) {

		Map<String, Set<String>> refs = new TreeMap<>();

		for (JsonNode dependencyJsonNode : jsonNode.get("dependencies")) {
			JsonNode refsJsonNode = dependencyJsonNode.get(fieldName);

			if (refsJsonNode == null) {
				continue;
			}

			Set<String> dependencyRefs = new TreeSet<>();

			for (JsonNode refJsonNode : refsJsonNode) {
				dependencyRefs.add(refJsonNode.asText());
			}

			refs.put(_getText(dependencyJsonNode, "ref"), dependencyRefs);
		}

		return refs;
	}

	private static Set<String> _getSettingNames(
		String bomRef, Map<String, JsonNode> componentJsonNodes,
		Map<String, Set<String>> dependsOn) {

		Set<String> settingNames = new TreeSet<>();

		for (String ref :
				dependsOn.getOrDefault(bomRef, Collections.emptySet())) {

			if (!ref.startsWith("setting:")) {
				continue;
			}

			settingNames.add(_getText(componentJsonNodes.get(ref), "name"));
		}

		return settingNames;
	}

	private static Set<String> _getTLSCipherSuites(
		JsonNode protocolPropertiesJsonNode) {

		Set<String> tlsCipherSuites = new TreeSet<>();

		if (protocolPropertiesJsonNode == null) {
			return tlsCipherSuites;
		}

		JsonNode cipherSuitesJsonNode = protocolPropertiesJsonNode.get(
			"cipherSuites");

		if (cipherSuitesJsonNode == null) {
			return tlsCipherSuites;
		}

		for (JsonNode cipherSuiteJsonNode : cipherSuitesJsonNode) {
			tlsCipherSuites.add(_getText(cipherSuiteJsonNode, "name"));
		}

		return tlsCipherSuites;
	}

	private static String _getText(JsonNode jsonNode, String fieldName) {
		JsonNode fieldJsonNode = jsonNode.get(fieldName);

		if (fieldJsonNode == null) {
			return null;
		}

		return fieldJsonNode.asText();
	}

	private static boolean _isEmpty(Map<String, Set<String>> changes) {
		for (Set<String> categoryChanges : changes.values()) {
			if (!categoryChanges.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	private static boolean _isFlaggedCurve(JsonNode jsonNode) {
		Map<String, String> properties = _getProperties(jsonNode);

		return Objects.equals(
			properties.get("liferay:cbom:non-nist-approved-curve"), "true");
	}

	private static JsonNode _normalize(JsonNode jsonNode) {
		ObjectNode objectNode = (ObjectNode)jsonNode.deepCopy();

		objectNode.remove(Arrays.asList("serialNumber", "version"));

		ObjectNode metadataObjectNode = (ObjectNode)objectNode.get("metadata");

		ObjectNode componentObjectNode = (ObjectNode)metadataObjectNode.get(
			"component");

		componentObjectNode.remove("version");

		return objectNode;
	}

	private static void _reviewComponents(
		Map<String, Set<String>> changes, JsonNode jsonNode,
		JsonNode previousJsonNode) {

		Map<String, JsonNode> componentJsonNodes = _getComponentJsonNodes(
			jsonNode);
		Map<String, Set<String>> dependsOn = _getRefs(jsonNode, "dependsOn");
		Map<String, JsonNode> previousComponentJsonNodes =
			_getComponentJsonNodes(previousJsonNode);
		Map<String, String> previousProviderNames = _getProviderNames(
			previousJsonNode);
		Map<String, String> providerNames = _getProviderNames(jsonNode);

		for (Map.Entry<String, JsonNode> entry :
				componentJsonNodes.entrySet()) {

			String bomRef = entry.getKey();
			JsonNode componentJsonNode = entry.getValue();

			JsonNode previousComponentJsonNode = previousComponentJsonNodes.get(
				bomRef);

			if (previousComponentJsonNode == null) {
				_reviewNewComponent(
					bomRef, changes, componentJsonNode,
					providerNames.get(bomRef),
					_getSettingNames(bomRef, componentJsonNodes, dependsOn));

				continue;
			}

			String label = _getLabel(bomRef, componentJsonNode);

			if (!Objects.equals(
					providerNames.get(bomRef),
					previousProviderNames.get(bomRef))) {

				_add(
					changes, _CATEGORY_CHANGED_PROVIDERS,
					_getChange(
						label, previousProviderNames.get(bomRef),
						providerNames.get(bomRef)));
			}

			if (componentJsonNode.equals(previousComponentJsonNode)) {
				continue;
			}

			String fipsApprovalStatus = _getFIPSApprovalStatus(
				componentJsonNode);
			String previousFIPSApprovalStatus = _getFIPSApprovalStatus(
				previousComponentJsonNode);

			if (!Objects.equals(
					fipsApprovalStatus, previousFIPSApprovalStatus)) {

				_add(
					changes, _CATEGORY_CHANGED_APPROVAL_STATUSES,
					_getChange(
						label, previousFIPSApprovalStatus, fipsApprovalStatus));
			}

			if (_isFlaggedCurve(componentJsonNode) &&
				!_isFlaggedCurve(previousComponentJsonNode)) {

				_add(changes, _CATEGORY_NEWLY_FLAGGED_CURVES, label);
			}

			if (bomRef.startsWith("provider:") &&
				!Objects.equals(
					_getText(componentJsonNode, "version"),
					_getText(previousComponentJsonNode, "version"))) {

				_add(
					changes, _CATEGORY_CHANGED_PROVIDERS,
					StringBundler.concat(
						label, ": version from ",
						_getText(previousComponentJsonNode, "version"), " to ",
						_getText(componentJsonNode, "version")));
			}

			JsonNode protocolPropertiesJsonNode =
				_getProtocolPropertiesJsonNode(componentJsonNode);

			if ((protocolPropertiesJsonNode != null) &&
				!protocolPropertiesJsonNode.equals(
					_getProtocolPropertiesJsonNode(
						previousComponentJsonNode))) {

				Set<String> previousTLSCipherSuites = _getTLSCipherSuites(
					_getProtocolPropertiesJsonNode(previousComponentJsonNode));
				Set<String> tlsCipherSuites = _getTLSCipherSuites(
					protocolPropertiesJsonNode);

				_add(
					changes, _CATEGORY_TLS_LIST_CHANGES,
					StringBundler.concat(
						label, ": adds ",
						new TreeSet<>(
							SetUtil.asymmetricDifference(
								tlsCipherSuites, previousTLSCipherSuites)),
						", removes ",
						new TreeSet<>(
							SetUtil.asymmetricDifference(
								previousTLSCipherSuites, tlsCipherSuites))));
			}

			_add(
				changes, _CATEGORY_CHANGED_ENTRIES,
				StringBundler.concat(
					label, ": ",
					StringUtil.merge(
						_getChangedFieldNames(
							componentJsonNode, previousComponentJsonNode),
						", ")));
		}

		for (Map.Entry<String, JsonNode> entry :
				previousComponentJsonNodes.entrySet()) {

			String bomRef = entry.getKey();

			if (componentJsonNodes.containsKey(bomRef)) {
				continue;
			}

			String label = _getLabel(bomRef, entry.getValue());

			_add(changes, _CATEGORY_REMOVED_ENTRIES, label);

			if (_getProtocolPropertiesJsonNode(entry.getValue()) == null) {
				continue;
			}

			_add(changes, _CATEGORY_TLS_LIST_CHANGES, "Removes " + label);
		}
	}

	private static void _reviewMetadata(
		Map<String, Set<String>> changes, JsonNode jsonNode,
		JsonNode previousJsonNode) {

		Map<String, String> properties = _getProperties(
			jsonNode.get("metadata"));
		Map<String, String> previousProperties = _getProperties(
			previousJsonNode.get("metadata"));

		for (String propertyName :
				_getChangedPropertyNames(properties, previousProperties)) {

			_add(
				changes, _CATEGORY_CHANGED_METADATA,
				_getChange(
					propertyName, previousProperties.get(propertyName),
					properties.get(propertyName)));
		}
	}

	private static void _reviewNewComponent(
		String bomRef, Map<String, Set<String>> changes,
		JsonNode componentJsonNode, String providerName,
		Set<String> settingNames) {

		String label = _getLabel(bomRef, componentJsonNode);

		if (bomRef.startsWith("provider:")) {
			String version = _getText(componentJsonNode, "version");

			if (version != null) {
				label = StringBundler.concat(label, ", version ", version);
			}

			_add(changes, _CATEGORY_CHANGED_PROVIDERS, "Adds " + label);

			return;
		}

		if (bomRef.startsWith("setting:")) {
			_add(changes, _CATEGORY_NEW_CONFIGURABLE_ENTRIES, label);

			return;
		}

		if (!bomRef.startsWith("crypto:")) {
			return;
		}

		Map<String, String> properties = _getProperties(componentJsonNode);

		String name = _getText(componentJsonNode, "name");

		if (Objects.equals(name, properties.get("liferay:cbom:jca-class"))) {
			_add(
				changes, _CATEGORY_NEW_UNRESOLVED_USES,
				StringBundler.concat(
					label, ": ", _getText(componentJsonNode, "description")));

			return;
		}

		if (settingNames.contains(name)) {
			_add(changes, _CATEGORY_NEW_CONFIGURABLE_ENTRIES, label);

			return;
		}

		if (_isFlaggedCurve(componentJsonNode)) {
			_add(changes, _CATEGORY_NEWLY_FLAGGED_CURVES, label);
		}

		JsonNode protocolPropertiesJsonNode = _getProtocolPropertiesJsonNode(
			componentJsonNode);

		if (protocolPropertiesJsonNode != null) {
			_add(
				changes, _CATEGORY_TLS_LIST_CHANGES,
				StringBundler.concat(
					"Adds ", label, " with ",
					_getTLSCipherSuites(protocolPropertiesJsonNode)));

			return;
		}

		String fipsApprovalStatus = _getFIPSApprovalStatus(componentJsonNode);

		if (fipsApprovalStatus != null) {
			label = StringBundler.concat(label, ": ", fipsApprovalStatus);
		}

		if (providerName != null) {
			label = StringBundler.concat(label, ", provider ", providerName);
		}

		_add(changes, _CATEGORY_NEW_ALGORITHMS, label);
	}

	private static void _reviewUses(
		Map<String, Set<String>> changes, JsonNode jsonNode,
		JsonNode previousJsonNode) {

		Map<String, Set<String>> dependsOn = _getRefs(jsonNode, "dependsOn");
		Map<String, Set<String>> previousDependsOn = _getRefs(
			previousJsonNode, "dependsOn");

		Set<String> refs = new TreeSet<>(dependsOn.keySet());

		refs.addAll(previousDependsOn.keySet());

		for (String ref : refs) {
			if (!ref.startsWith("module:")) {
				continue;
			}

			Set<String> bomRefs = dependsOn.getOrDefault(
				ref, Collections.emptySet());
			Set<String> previousBOMRefs = previousDependsOn.getOrDefault(
				ref, Collections.emptySet());

			for (String bomRef :
					SetUtil.asymmetricDifference(bomRefs, previousBOMRefs)) {

				_add(changes, _CATEGORY_CHANGED_USES, ref + " uses " + bomRef);
			}

			for (String bomRef :
					SetUtil.asymmetricDifference(previousBOMRefs, bomRefs)) {

				_add(
					changes, _CATEGORY_CHANGED_USES,
					ref + " no longer uses " + bomRef);
			}
		}
	}

	private static final String _CATEGORY_CHANGED_APPROVAL_STATUSES =
		"Changed approval statuses";

	private static final String _CATEGORY_CHANGED_ENTRIES = "Changed entries";

	private static final String _CATEGORY_CHANGED_METADATA = "Changed metadata";

	private static final String _CATEGORY_CHANGED_PROVIDERS =
		"Changed providers";

	private static final String _CATEGORY_CHANGED_USES = "Changed uses";

	private static final String _CATEGORY_NEW_ALGORITHMS = "New algorithms";

	private static final String _CATEGORY_NEW_CONFIGURABLE_ENTRIES =
		"New configurable entries";

	private static final String _CATEGORY_NEW_UNRESOLVED_USES =
		"New uses the scan cannot resolve";

	private static final String _CATEGORY_NEWLY_FLAGGED_CURVES =
		"Newly flagged curves";

	private static final String _CATEGORY_REMOVED_ENTRIES = "Removed entries";

	private static final String _CATEGORY_TLS_LIST_CHANGES = "TLS list changes";

	private static final String _EXTENSION = ".cdx.json";

	private static final List<String> _categories = Arrays.asList(
		_CATEGORY_NEW_ALGORITHMS, _CATEGORY_CHANGED_PROVIDERS,
		_CATEGORY_CHANGED_APPROVAL_STATUSES, _CATEGORY_NEWLY_FLAGGED_CURVES,
		_CATEGORY_NEW_CONFIGURABLE_ENTRIES, _CATEGORY_TLS_LIST_CHANGES,
		_CATEGORY_NEW_UNRESOLVED_USES, _CATEGORY_CHANGED_USES,
		_CATEGORY_REMOVED_ENTRIES, _CATEGORY_CHANGED_ENTRIES,
		_CATEGORY_CHANGED_METADATA);

}