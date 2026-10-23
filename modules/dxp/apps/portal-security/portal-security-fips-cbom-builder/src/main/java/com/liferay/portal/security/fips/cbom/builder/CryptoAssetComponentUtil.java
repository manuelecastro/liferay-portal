/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.SetUtil;
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

	public static JSONArray toJSONArray(
			List<CryptoUsage> cryptoUsages, Properties portalProperties)
		throws Exception {

		Map<String, List<CryptoUsage>> cryptoUsagesMap = new TreeMap<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			List<CryptoUsage> bomRefCryptoUsages =
				cryptoUsagesMap.computeIfAbsent(
					_getBOMRef(cryptoUsage), bomRef -> new ArrayList<>());

			bomRefCryptoUsages.add(cryptoUsage);
		}

		return JSONUtil.toJSONArray(
			cryptoUsagesMap.entrySet(),
			entry -> _toJSONObject(
				entry.getKey(), entry.getValue(), portalProperties));
	}

	private static void _addProperty(
		String name, Map<String, Set<String>> propertiesMap, String value) {

		Set<String> values = propertiesMap.computeIfAbsent(
			"liferay:cbom:" + name, key -> new TreeSet<>());

		values.add(value);
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

	private static Set<CryptoFunction> _getCryptoFunctions(
		List<CryptoUsage> cryptoUsages) {

		Set<CryptoFunction> cryptoFunctions = new TreeSet<>();

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			cryptoFunctions.addAll(cryptoUsage.getCryptoFunctions());
		}

		return cryptoFunctions;
	}

	private static CryptoPrimitive _getCryptoPrimitive(
		List<CryptoUsage> cryptoUsages) {

		CryptoUsage firstCryptoUsage = cryptoUsages.get(0);

		for (CryptoUsage cryptoUsage : cryptoUsages) {
			if (cryptoUsage.getCryptoPrimitive() !=
					firstCryptoUsage.getCryptoPrimitive()) {

				return CryptoPrimitive.UNKNOWN;
			}
		}

		return firstCryptoUsage.getCryptoPrimitive();
	}

	private static String _getName(CryptoUsage cryptoUsage) {
		if (cryptoUsage.isConfigurable()) {
			List<String> configuredBy = cryptoUsage.getConfiguredBy();

			return configuredBy.get(0);
		}

		return cryptoUsage.getAlgorithm();
	}

	private static void _putCipherProperties(
		JSONObject algorithmPropertiesJSONObject, String transformation) {

		String[] transformationParts = StringUtil.split(
			StringUtil.toUpperCase(transformation), CharPool.SLASH);

		if (transformationParts.length != 3) {
			return;
		}

		if (_modes.contains(transformationParts[1])) {
			algorithmPropertiesJSONObject.put(
				"mode", StringUtil.toLowerCase(transformationParts[1]));
		}
		else {
			algorithmPropertiesJSONObject.put("mode", "other");
		}

		String padding = transformationParts[2];

		if (padding.equals("NOPADDING")) {
			return;
		}

		if (padding.startsWith("OAEP")) {
			algorithmPropertiesJSONObject.put("padding", "oaep");
		}
		else if (padding.equals("PKCS1PADDING")) {
			algorithmPropertiesJSONObject.put("padding", "pkcs1v15");
		}
		else if (padding.equals("PKCS5PADDING")) {
			algorithmPropertiesJSONObject.put("padding", "pkcs5");
		}
		else if (padding.equals("PKCS7PADDING")) {
			algorithmPropertiesJSONObject.put("padding", "pkcs7");
		}
		else {
			algorithmPropertiesJSONObject.put("padding", "other");
		}
	}

	private static JSONObject _toJSONObject(
			String bomRef, List<CryptoUsage> cryptoUsages,
			Properties portalProperties)
		throws Exception {

		CryptoPrimitive cryptoPrimitive = _getCryptoPrimitive(cryptoUsages);

		JSONObject algorithmPropertiesJSONObject = JSONUtil.put(
			"cryptoFunctions",
			JSONUtil.toJSONArray(
				_getCryptoFunctions(cryptoUsages), CryptoFunction::getValue)
		).put(
			"primitive", cryptoPrimitive.getValue()
		);

		Set<String> classNames = new TreeSet<>();
		Map<String, Set<String>> propertiesMap = new TreeMap<>();

		for (CryptoUsage curCryptoUsage : cryptoUsages) {
			classNames.addAll(curCryptoUsage.getClassNames());

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
			if (cryptoUsage.getCurve() != null) {
				algorithmPropertiesJSONObject.put(
					"curve", cryptoUsage.getCurve());
			}

			if (cryptoUsage.getKeySize() > 0) {
				algorithmPropertiesJSONObject.put(
					"parameterSetIdentifier",
					String.valueOf(cryptoUsage.getKeySize()));
			}

			if (Objects.equals(cryptoUsage.getJCAClassName(), "Cipher")) {
				_putCipherProperties(
					algorithmPropertiesJSONObject, cryptoUsage.getAlgorithm());
			}

			_addProperty(
				"provider", propertiesMap,
				CryptoProviderUtil.getProviderName(
					cryptoUsage.getAlgorithm(), cryptoUsage.getJCAClassName()));
		}

		List<JSONObject> propertyJSONObjects = new ArrayList<>();

		for (Map.Entry<String, Set<String>> entry : propertiesMap.entrySet()) {
			for (String value : entry.getValue()) {
				propertyJSONObjects.add(
					JSONUtil.put(
						"name", entry.getKey()
					).put(
						"value", value
					));
			}
		}

		return JSONUtil.put(
			"bom-ref", bomRef
		).put(
			"cryptoProperties",
			JSONUtil.put(
				"algorithmProperties", algorithmPropertiesJSONObject
			).put(
				"assetType", "algorithm"
			)
		).put(
			"evidence",
			JSONUtil.put(
				"occurrences",
				JSONUtil.toJSONArray(
					classNames,
					className -> JSONUtil.put("location", className)))
		).put(
			"name", _getName(cryptoUsage)
		).put(
			"properties", JSONUtil.putAll(propertyJSONObjects.toArray())
		).put(
			"type", "cryptographic-asset"
		);
	}

	private static final Set<String> _modes = SetUtil.fromArray(
		"CBC", "CCM", "CFB", "CTR", "ECB", "GCM", "OFB");

}