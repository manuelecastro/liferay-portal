/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;

import java.util.Collections;
import java.util.List;

/**
 * Turns the scanned crypto calls into crypto usages, each with the primitive
 * of its algorithm. A call whose algorithm the scan cannot resolve keeps the
 * reason, which says where the algorithm comes from, so the CBOM can describe
 * it instead of leaving it out.
 *
 * @author Manuele Castro
 */
public class CryptoUsageUtil {

	public static List<CryptoUsage> toCryptoUsages(
		List<CryptoCall> cryptoCalls) {

		return TransformUtil.transform(
			cryptoCalls, CryptoUsageUtil::_toCryptoUsage);
	}

	private static CryptoUsage _toCryptoUsage(CryptoCall cryptoCall) {
		CryptoUsage cryptoUsage = new CryptoUsage(
			cryptoCall.getBundleSymbolicName(),
			Collections.singletonList(cryptoCall.getClassName()),
			cryptoCall.getCryptoFunctions(), cryptoCall.getFeatureName(),
			cryptoCall.getJCAClassName(),
			CryptoPrimitiveUtil.getPrimitive(
				cryptoCall.getAlgorithm(), cryptoCall.getJCAClassName()));

		cryptoUsage.setAlgorithm(cryptoCall.getAlgorithm());
		cryptoUsage.setConfiguredBy(cryptoCall.getConfiguredBy());
		cryptoUsage.setKeySize(cryptoCall.getKeySize());
		cryptoUsage.setRelatedCryptoMaterialTypes(
			cryptoCall.getRelatedCryptoMaterialTypes());
		cryptoUsage.setUnresolvedReason(cryptoCall.getUnresolvedReason());

		return cryptoUsage;
	}

}