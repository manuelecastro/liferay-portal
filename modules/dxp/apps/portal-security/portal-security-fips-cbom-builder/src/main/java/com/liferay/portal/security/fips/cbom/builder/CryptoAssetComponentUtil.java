/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.crypto.AlgorithmProperties;
import org.cyclonedx.model.component.crypto.CryptoProperties;
import org.cyclonedx.model.component.crypto.enums.AssetType;
import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Mode;
import org.cyclonedx.model.component.crypto.enums.Padding;
import org.cyclonedx.model.component.crypto.enums.Primitive;
import org.cyclonedx.model.component.evidence.Occurrence;

/**
 * Turns the usage data into CycloneDX 1.6 <code>cryptographic-asset</code>
 * components. Uses of the same JCA class and algorithm share one component,
 * which lists every feature and class that uses it. Uses whose algorithm a
 * portal property chooses share one component per property, which names the
 * property instead of an algorithm.
 *
 * @author Manuele Castro
 */
public class CryptoAssetComponentUtil {

	public static List<Component> toComponents(
			List<CryptoUsage> cryptoUsages, Properties portalProperties)
		throws Exception {

		Map<String, List<CryptoUsage>> cryptoUsagesMap = new TreeMap<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			List<CryptoUsage> bomRefCryptoUsages =
				cryptoUsagesMap.computeIfAbsent(
					_getBOMRef(cryptoUsage), bomRef -> new ArrayList<>());

			bomRefCryptoUsages.add(cryptoUsage);
		}

		return TransformUtil.unsafeTransform(
			cryptoUsagesMap.entrySet(),
			entry -> _toComponent(
				entry.getKey(), entry.getValue(), portalProperties));
	}

	private static void _addProperty(
		String name, Map<String, Set<String>> propertiesMap, String value) {

		Set<String> values = propertiesMap.computeIfAbsent(
			"liferay:cbom:" + name, key -> new TreeSet<>());

		values.add(value);
	}

	private static AlgorithmProperties _getAlgorithmProperties(
		List<CryptoUsage> cryptoUsages) {

		AlgorithmProperties algorithmProperties = new AlgorithmProperties();

		algorithmProperties.setCryptoFunctions(
			_getCryptoFunctions(cryptoUsages));
		algorithmProperties.setPrimitive(_getPrimitive(cryptoUsages));

		CryptoUsage cryptoUsage = cryptoUsages.get(0);

		if (cryptoUsage.isConfigurable()) {
			return algorithmProperties;
		}

		algorithmProperties.setCurve(cryptoUsage.getCurve());

		if (cryptoUsage.getKeySize() > 0) {
			algorithmProperties.setParameterSetIdentifier(
				String.valueOf(cryptoUsage.getKeySize()));
		}

		if (Objects.equals(cryptoUsage.getJCAClassName(), "Cipher")) {
			_setCipherProperties(
				algorithmProperties, cryptoUsage.getAlgorithm());
		}

		return algorithmProperties;
	}

	private static String _getBOMRef(CryptoUsage cryptoUsage) {
		if (cryptoUsage.isConfigurable()) {
			List<String> configuredBy = cryptoUsage.getConfiguredBy();

			return "crypto:" + configuredBy.get(0);
		}

		StringBundler sb = new StringBundler(8);

		sb.append("crypto:");
		sb.append(cryptoUsage.getJCAClassName());
		sb.append(StringPool.COLON);
		sb.append(cryptoUsage.getAlgorithm());

		if (cryptoUsage.getKeySize() > 0) {
			sb.append(StringPool.COLON);
			sb.append(cryptoUsage.getKeySize());
		}

		if (cryptoUsage.getCurve() != null) {
			sb.append(StringPool.COLON);
			sb.append(cryptoUsage.getCurve());
		}

		return sb.toString();
	}

	private static List<CryptoFunction> _getCryptoFunctions(
		List<CryptoUsage> cryptoUsages) {

		Set<CryptoFunction> cryptoFunctions = new TreeSet<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			cryptoFunctions.addAll(cryptoUsage.getCryptoFunctions());
		}

		return new ArrayList<>(cryptoFunctions);
	}

	private static Evidence _getEvidence(List<CryptoUsage> cryptoUsages) {
		Evidence evidence = new Evidence();

		Set<String> classNames = new TreeSet<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			classNames.addAll(cryptoUsage.getClassNames());
		}

		evidence.setOccurrences(
			TransformUtil.transform(
				classNames,
				className -> {
					Occurrence occurrence = new Occurrence();

					occurrence.setLocation(className);

					return occurrence;
				}));

		return evidence;
	}

	private static Mode _getMode(String name) {
		for (Mode mode : Mode.values()) {
			if (StringUtil.equalsIgnoreCase(mode.getName(), name)) {
				return mode;
			}
		}

		return Mode.OTHER;
	}

	private static String _getName(CryptoUsage cryptoUsage) {
		if (cryptoUsage.isConfigurable()) {
			List<String> configuredBy = cryptoUsage.getConfiguredBy();

			return configuredBy.get(0);
		}

		return cryptoUsage.getAlgorithm();
	}

	private static Primitive _getPrimitive(List<CryptoUsage> cryptoUsages) {
		CryptoUsage firstCryptoUsage = cryptoUsages.get(0);

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			if (cryptoUsage.getPrimitive() != firstCryptoUsage.getPrimitive()) {
				return Primitive.UNKNOWN;
			}
		}

		return firstCryptoUsage.getPrimitive();
	}

	private static List<Property> _getProperties(
			List<CryptoUsage> cryptoUsages, Properties portalProperties)
		throws Exception {

		List<Property> properties = new ArrayList<>();

		Map<String, Set<String>> propertiesMap = new TreeMap<>();

		for (CryptoUsage curCryptoUsage : cryptoUsages) {
			_addProperty(
				"feature", propertiesMap, curCryptoUsage.getFeatureName());
			_addProperty(
				"jca-class", propertiesMap, curCryptoUsage.getJCAClassName());

			for (String propertyName : curCryptoUsage.getConfiguredBy()) {
				_addProperty("configured-by", propertiesMap, propertyName);

				String defaultValue = portalProperties.getProperty(
					propertyName);

				if (Validator.isNull(defaultValue)) {
					continue;
				}

				_addProperty(
					"default-value", propertiesMap,
					propertyName + StringPool.EQUAL + defaultValue);
			}
		}

		CryptoUsage cryptoUsage = cryptoUsages.get(0);

		if (!cryptoUsage.isConfigurable()) {
			_addProperty(
				"provider", propertiesMap,
				CryptoProviderUtil.getProviderName(
					cryptoUsage.getAlgorithm(), cryptoUsage.getJCAClassName()));
		}

		for (Map.Entry<String, Set<String>> entry : propertiesMap.entrySet()) {
			for (String value : entry.getValue()) {
				properties.add(new Property(entry.getKey(), value));
			}
		}

		return properties;
	}

	private static void _setCipherProperties(
		AlgorithmProperties algorithmProperties, String transformation) {

		String[] transformationParts = StringUtil.split(
			StringUtil.toUpperCase(transformation), CharPool.SLASH);

		if (transformationParts.length != 3) {
			return;
		}

		algorithmProperties.setMode(_getMode(transformationParts[1]));

		String paddingName = transformationParts[2];

		if (paddingName.equals("NOPADDING")) {
			return;
		}

		if (paddingName.startsWith("OAEP")) {
			algorithmProperties.setPadding(Padding.OAEP);
		}
		else if (paddingName.equals("PKCS1PADDING")) {
			algorithmProperties.setPadding(Padding.PKCS1V15);
		}
		else if (paddingName.equals("PKCS5PADDING")) {
			algorithmProperties.setPadding(Padding.PKCS5);
		}
		else if (paddingName.equals("PKCS7PADDING")) {
			algorithmProperties.setPadding(Padding.PKCS7);
		}
		else {
			algorithmProperties.setPadding(Padding.OTHER);
		}
	}

	private static Component _toComponent(
			String bomRef, List<CryptoUsage> cryptoUsages,
			Properties portalProperties)
		throws Exception {

		Component component = new Component();

		CryptoProperties cryptoProperties = new CryptoProperties();

		cryptoProperties.setAlgorithmProperties(
			_getAlgorithmProperties(cryptoUsages));
		cryptoProperties.setAssetType(AssetType.ALGORITHM);

		component.setBomRef(bomRef);
		component.setCryptoProperties(cryptoProperties);
		component.setEvidence(_getEvidence(cryptoUsages));
		component.setName(_getName(cryptoUsages.get(0)));
		component.setProperties(_getProperties(cryptoUsages, portalProperties));
		component.setType(Component.Type.CRYPTOGRAPHIC_ASSET);

		return component;
	}

}