/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringPool;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.LifecycleChoice;
import org.cyclonedx.model.Lifecycles;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.data.ComponentData;

/**
 * @author Manuele Castro
 */
public class CycloneDXCBOMWriter {

	public static String write(
			String configurationName, List<CryptoCall> cryptoCalls,
			String productName, String productVersion)
		throws Exception {

		Bom bom = new Bom();

		bom.setComponents(_getComponents(cryptoCalls));
		bom.setDependencies(_getDependencies(cryptoCalls));
		bom.setMetadata(
			_getMetadata(configurationName, productName, productVersion));
		bom.setSerialNumber(
			_getSerialNumber(configurationName, productVersion));
		bom.setVersion(1);

		BomJsonGenerator bomJsonGenerator = BomGeneratorFactory.createJson(
			Version.VERSION_16, bom);

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode jsonNode = objectMapper.readTree(
			bomJsonGenerator.toJsonString(false));

		JsonNode dependenciesJsonNode = jsonNode.get("dependencies");

		if (dependenciesJsonNode != null) {
			Map<String, Set<String>> provides = _getProvides(cryptoCalls);

			for (JsonNode dependencyJsonNode : dependenciesJsonNode) {
				JsonNode refJsonNode = dependencyJsonNode.get("ref");

				Set<String> bomRefs = provides.get(refJsonNode.asText());

				if (bomRefs == null) {
					continue;
				}

				ObjectNode objectNode = (ObjectNode)dependencyJsonNode;

				ArrayNode arrayNode = objectNode.putArray("provides");

				for (String bomRef : bomRefs) {
					arrayNode.add(bomRef);
				}
			}
		}

		DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter();

		defaultPrettyPrinter.indentArraysWith(
			DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

		ObjectWriter objectWriter = objectMapper.writer(defaultPrettyPrinter);

		return objectWriter.writeValueAsString(jsonNode);
	}

	private static String _fetchProviderName(CryptoCall cryptoCall) {
		if (cryptoCall.getAlgorithm() == null) {
			return null;
		}

		return CryptoProviderUtil.fetchProviderName(
			cryptoCall.getAlgorithm(), cryptoCall.getJCAClassName());
	}

	private static List<Component> _getComponents(
		List<CryptoCall> cryptoCalls) {

		Map<String, Component> componentsMap = new TreeMap<>();

		for (Component component :
				CryptoAssetComponentUtil.toComponents(cryptoCalls)) {

			componentsMap.put(component.getBomRef(), component);
		}

		for (CryptoCall cryptoCall : cryptoCalls) {
			componentsMap.computeIfAbsent(
				"module:" + cryptoCall.getBundleSymbolicName(),
				bomRef -> _toLibraryComponent(
					bomRef, cryptoCall.getFeatureName()));

			String providerName = _fetchProviderName(cryptoCall);

			if (providerName != null) {
				componentsMap.computeIfAbsent(
					"provider:" + providerName,
					bomRef -> _toLibraryComponent(bomRef, providerName));
			}

			for (String propertyKey : cryptoCall.getConfiguredBy()) {
				componentsMap.computeIfAbsent(
					"setting:" + propertyKey,
					bomRef -> _toDataComponent(bomRef, propertyKey));
			}
		}

		return new ArrayList<>(componentsMap.values());
	}

	private static List<Dependency> _getDependencies(
		List<CryptoCall> cryptoCalls) {

		Map<String, Set<String>> dependsOnMap = new TreeMap<>();

		for (CryptoCall cryptoCall : cryptoCalls) {
			String bomRef = CryptoAssetComponentUtil.getBOMRef(cryptoCall);

			Set<String> bomRefDependsOn = dependsOnMap.computeIfAbsent(
				bomRef, key -> new TreeSet<>());

			for (String propertyKey : cryptoCall.getConfiguredBy()) {
				bomRefDependsOn.add("setting:" + propertyKey);

				dependsOnMap.computeIfAbsent(
					"setting:" + propertyKey, key -> new TreeSet<>());
			}

			Set<String> moduleDependsOn = dependsOnMap.computeIfAbsent(
				"module:" + cryptoCall.getBundleSymbolicName(),
				key -> new TreeSet<>());

			moduleDependsOn.add(bomRef);

			String providerName = _fetchProviderName(cryptoCall);

			if (providerName == null) {
				continue;
			}

			dependsOnMap.computeIfAbsent(
				"provider:" + providerName, key -> new TreeSet<>());
		}

		return TransformUtil.transform(
			dependsOnMap.entrySet(),
			entry -> {
				Dependency dependency = new Dependency(entry.getKey());

				dependency.setDependencies(
					TransformUtil.transform(entry.getValue(), Dependency::new));

				return dependency;
			});
	}

	private static Metadata _getMetadata(
		String configurationName, String productName, String productVersion) {

		Metadata metadata = new Metadata();

		Component component = new Component();

		component.setName(productName);
		component.setType(Component.Type.APPLICATION);
		component.setVersion(productVersion);

		LifecycleChoice lifecycleChoice = new LifecycleChoice();

		lifecycleChoice.setPhase(LifecycleChoice.Phase.POST_BUILD);

		Lifecycles lifecycles = new Lifecycles();

		lifecycles.setLifecycleChoice(
			Collections.singletonList(lifecycleChoice));

		metadata.setComponent(component);
		metadata.setLifecycles(lifecycles);
		metadata.setProperties(
			Arrays.asList(
				new Property("liferay:cbom:configuration", configurationName),
				new Property("liferay:cbom:schema-version", "1.0")));
		metadata.setTimestamp(null);

		return metadata;
	}

	private static Map<String, Set<String>> _getProvides(
		List<CryptoCall> cryptoCalls) {

		Map<String, Set<String>> provides = new TreeMap<>();

		for (CryptoCall cryptoCall : cryptoCalls) {
			String providerName = _fetchProviderName(cryptoCall);

			if (providerName == null) {
				continue;
			}

			Set<String> bomRefs = provides.computeIfAbsent(
				"provider:" + providerName, key -> new TreeSet<>());

			bomRefs.add(CryptoAssetComponentUtil.getBOMRef(cryptoCall));
		}

		return provides;
	}

	private static String _getSerialNumber(
		String configurationName, String productVersion) {

		String name = productVersion + StringPool.POUND + configurationName;

		return "urn:uuid:" +
			UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
	}

	private static Component _toDataComponent(
		String bomRef, String propertyKey) {

		ComponentData componentData = new ComponentData();

		if (CryptoCallScanner.isJDKPropertyKey(propertyKey)) {
			componentData.setName("java.security");
		}
		else {
			componentData.setName("portal.properties");
		}

		componentData.setType(ComponentData.ComponentDataType.CONFIGURATION);

		Component component = new Component();

		component.setBomRef(bomRef);
		component.setData(Collections.singletonList(componentData));
		component.setName(propertyKey);
		component.setType(Component.Type.DATA);

		return component;
	}

	private static Component _toLibraryComponent(String bomRef, String name) {
		Component component = new Component();

		component.setBomRef(bomRef);
		component.setName(name);
		component.setType(Component.Type.LIBRARY);

		return component;
	}

}