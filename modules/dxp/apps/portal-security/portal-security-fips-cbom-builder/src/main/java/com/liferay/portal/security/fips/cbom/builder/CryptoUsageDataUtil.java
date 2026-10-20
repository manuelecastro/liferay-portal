/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Objects;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;

/**
 * Reads the usage data, which describes, for each feature, the classes that
 * call cryptography and what they call. The crypto functions and the primitive
 * are the values of the CycloneDX schema.
 *
 * @author Manuele Castro
 */
public class CryptoUsageDataUtil {

	public static List<CryptoUsage> getCryptoUsages(JSONObject jsonObject)
		throws Exception {

		return JSONUtil.toList(
			jsonObject.getJSONArray("cryptoUsages"),
			CryptoUsageDataUtil::_toCryptoUsage);
	}

	private static CryptoFunction _getCryptoFunction(
		String featureName, String name) {

		for (CryptoFunction cryptoFunction : CryptoFunction.values()) {
			if (Objects.equals(cryptoFunction.getName(), name)) {
				return cryptoFunction;
			}
		}

		throw new IllegalArgumentException(
			StringBundler.concat(
				"The crypto usage of ", featureName,
				" has an invalid crypto function ", name));
	}

	private static Primitive _getPrimitive(String featureName, String name) {
		for (Primitive primitive : Primitive.values()) {
			if (Objects.equals(primitive.getName(), name)) {
				return primitive;
			}
		}

		throw new IllegalArgumentException(
			StringBundler.concat(
				"The crypto usage of ", featureName,
				" has an invalid primitive ", name));
	}

	private static CryptoUsage _toCryptoUsage(JSONObject jsonObject) {
		String featureName = jsonObject.getString("featureName");

		if (Validator.isNull(featureName)) {
			throw new IllegalArgumentException(
				"A crypto usage has no feature name");
		}

		List<String> classNames = JSONUtil.toStringList(
			jsonObject.getJSONArray("classNames"));

		if (ListUtil.isEmpty(classNames)) {
			throw new IllegalArgumentException(
				"The crypto usage of " + featureName + " has no class names");
		}

		List<CryptoFunction> cryptoFunctions = TransformUtil.unsafeTransform(
			JSONUtil.toStringList(jsonObject.getJSONArray("cryptoFunctions")),
			cryptoFunctionName -> _getCryptoFunction(
				featureName, cryptoFunctionName));

		if (ListUtil.isEmpty(cryptoFunctions)) {
			throw new IllegalArgumentException(
				"The crypto usage of " + featureName +
					" has no crypto functions");
		}

		String jcaClassName = jsonObject.getString("jcaClassName");

		if (!CryptoProviderUtil.isSupportedJCAClassName(jcaClassName)) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"The crypto usage of ", featureName,
					" has an unsupported JCA class ", jcaClassName));
		}

		CryptoUsage cryptoUsage = new CryptoUsage(
			classNames, cryptoFunctions, featureName, jcaClassName,
			_getPrimitive(featureName, jsonObject.getString("primitive")));

		String algorithm = jsonObject.getString("algorithm");
		List<String> configuredBy = JSONUtil.toStringList(
			jsonObject.getJSONArray("configuredBy"));

		if (Validator.isNull(algorithm) == ListUtil.isEmpty(configuredBy)) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"The crypto usage of ", featureName,
					" needs either an algorithm or the portal properties that ",
					"configure it"));
		}

		if (Validator.isNotNull(algorithm)) {
			cryptoUsage.setAlgorithm(algorithm);
		}
		else {
			cryptoUsage.setConfiguredBy(configuredBy);
		}

		cryptoUsage.setCurve(jsonObject.getString("curve", null));
		cryptoUsage.setKeySize(jsonObject.getInt("keySize"));

		return cryptoUsage;
	}

}