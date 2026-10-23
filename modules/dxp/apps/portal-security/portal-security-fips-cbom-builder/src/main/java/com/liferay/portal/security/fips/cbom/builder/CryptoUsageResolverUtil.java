/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.StringBundler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns the scanned crypto calls into crypto usages. A call whose algorithm the
 * scan resolves becomes a usage of its own. A call it cannot resolve takes its
 * algorithm or settings from the usage data, which describes it by its class
 * and JCA class. A call that the usage data does not describe, and a
 * description that no such call needs, are problems.
 *
 * @author Manuele Castro
 */
public class CryptoUsageResolverUtil {

	public static List<CryptoUsage> getCryptoUsages(
		List<CryptoCall> cryptoCalls, List<CryptoUsage> describedCryptoUsages,
		Set<String> problems) {

		List<CryptoUsage> cryptoUsages = new ArrayList<>();

		Map<String, CryptoUsage> describedCryptoUsagesMap = new HashMap<>();

		for (CryptoUsage describedCryptoUsage : describedCryptoUsages) {
			for (String className : describedCryptoUsage.getClassNames()) {
				describedCryptoUsagesMap.put(
					_getDescription(
						className, describedCryptoUsage.getJCAClassName()),
					describedCryptoUsage);
			}
		}

		Set<String> unusedDescriptions = new TreeSet<>(
			describedCryptoUsagesMap.keySet());

		for (CryptoCall cryptoCall : cryptoCalls) {
			if (cryptoCall.isResolved()) {
				cryptoUsages.add(_toCryptoUsage(cryptoCall));

				continue;
			}

			String description = _getDescription(
				cryptoCall.getClassName(), cryptoCall.getJCAClassName());

			CryptoUsage describedCryptoUsage = describedCryptoUsagesMap.get(
				description);

			if (describedCryptoUsage == null) {
				problems.add(
					StringBundler.concat(
						"Unable to resolve the algorithm of ", description,
						", which comes from ",
						cryptoCall.getUnresolvedReason()));

				continue;
			}

			unusedDescriptions.remove(description);

			cryptoUsages.add(_toCryptoUsage(cryptoCall, describedCryptoUsage));
		}

		for (String unusedDescription : unusedDescriptions) {
			problems.add(
				StringBundler.concat(
					"The usage data describes ", unusedDescription,
					", but the scan finds no such call whose algorithm it ",
					"cannot resolve"));
		}

		return cryptoUsages;
	}

	private static String _getDescription(
		String className, String jcaClassName) {

		return StringBundler.concat(
			"the ", jcaClassName, " call in ", className);
	}

	private static CryptoUsage _toCryptoUsage(CryptoCall cryptoCall) {
		CryptoUsage cryptoUsage = new CryptoUsage(
			Collections.singletonList(cryptoCall.getClassName()),
			cryptoCall.getCryptoFunctions(), cryptoCall.getFeatureName(),
			cryptoCall.getJCAClassName(),
			CryptoPrimitiveUtil.getPrimitive(
				cryptoCall.getAlgorithm(), cryptoCall.getJCAClassName()));

		cryptoUsage.setAlgorithm(cryptoCall.getAlgorithm());
		cryptoUsage.setConfiguredBy(cryptoCall.getConfiguredBy());
		cryptoUsage.setKeySize(cryptoCall.getKeySize());

		return cryptoUsage;
	}

	private static CryptoUsage _toCryptoUsage(
		CryptoCall cryptoCall, CryptoUsage describedCryptoUsage) {

		CryptoUsage cryptoUsage = new CryptoUsage(
			Collections.singletonList(cryptoCall.getClassName()),
			describedCryptoUsage.getCryptoFunctions(),
			cryptoCall.getFeatureName(), cryptoCall.getJCAClassName(),
			describedCryptoUsage.getPrimitive());

		cryptoUsage.setAlgorithm(describedCryptoUsage.getAlgorithm());
		cryptoUsage.setConfiguredBy(describedCryptoUsage.getConfiguredBy());
		cryptoUsage.setCurve(describedCryptoUsage.getCurve());
		cryptoUsage.setKeySize(describedCryptoUsage.getKeySize());

		return cryptoUsage;
	}

}