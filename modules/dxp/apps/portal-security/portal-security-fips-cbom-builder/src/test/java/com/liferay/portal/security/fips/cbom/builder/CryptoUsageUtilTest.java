/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;
import org.cyclonedx.model.component.crypto.enums.RelatedCryptoMaterialType;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoUsageUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testToCryptoUsages() {
		String bundleSymbolicName = RandomTestUtil.randomString();
		String featureName = RandomTestUtil.randomString();

		CryptoCall resolvedCryptoCall = new CryptoCall(
			bundleSymbolicName, "com.liferay.test.Class1", featureName,
			"MessageDigest");

		resolvedCryptoCall.setAlgorithm("SHA-256");
		resolvedCryptoCall.setCryptoFunctions(
			Collections.singletonList(CryptoFunction.DIGEST));

		CryptoCall configurableCryptoCall = new CryptoCall(
			bundleSymbolicName, RandomTestUtil.randomString(), featureName,
			"KeyFactory");

		configurableCryptoCall.setConfiguredBy(
			Collections.singletonList("test.algorithm"));
		configurableCryptoCall.setCryptoFunctions(
			Collections.singletonList(CryptoFunction.OTHER));
		configurableCryptoCall.setRelatedCryptoMaterialTypes(
			Collections.singletonList(RelatedCryptoMaterialType.PUBLIC_KEY));

		CryptoCall unresolvedCryptoCall = new CryptoCall(
			bundleSymbolicName, RandomTestUtil.randomString(), featureName,
			"Mac");

		String unresolvedReason = RandomTestUtil.randomString();

		unresolvedCryptoCall.setCryptoFunctions(
			Collections.singletonList(CryptoFunction.TAG));
		unresolvedCryptoCall.setUnresolvedReason(unresolvedReason);

		List<CryptoUsage> cryptoUsages = CryptoUsageUtil.toCryptoUsages(
			Arrays.asList(
				resolvedCryptoCall, configurableCryptoCall,
				unresolvedCryptoCall));

		Assert.assertEquals(cryptoUsages.toString(), 3, cryptoUsages.size());

		CryptoUsage cryptoUsage = cryptoUsages.get(0);

		Assert.assertEquals("SHA-256", cryptoUsage.getAlgorithm());
		Assert.assertEquals(
			bundleSymbolicName, cryptoUsage.getBundleSymbolicName());
		Assert.assertEquals(
			Collections.singletonList("com.liferay.test.Class1"),
			cryptoUsage.getClassNames());
		Assert.assertEquals(
			Collections.emptyList(), cryptoUsage.getConfiguredBy());
		Assert.assertEquals(
			Collections.singletonList(CryptoFunction.DIGEST),
			cryptoUsage.getCryptoFunctions());
		Assert.assertEquals(featureName, cryptoUsage.getFeatureName());
		Assert.assertEquals("MessageDigest", cryptoUsage.getJCAClassName());
		Assert.assertEquals(Primitive.HASH, cryptoUsage.getPrimitive());
		Assert.assertNull(cryptoUsage.getUnresolvedReason());
		Assert.assertTrue(cryptoUsage.isResolved());

		cryptoUsage = cryptoUsages.get(1);

		Assert.assertNull(cryptoUsage.getAlgorithm());
		Assert.assertEquals(
			Collections.singletonList("test.algorithm"),
			cryptoUsage.getConfiguredBy());
		Assert.assertEquals(Primitive.UNKNOWN, cryptoUsage.getPrimitive());
		Assert.assertEquals(
			Collections.singletonList(RelatedCryptoMaterialType.PUBLIC_KEY),
			cryptoUsage.getRelatedCryptoMaterialTypes());
		Assert.assertTrue(cryptoUsage.isResolved());

		cryptoUsage = cryptoUsages.get(2);

		Assert.assertNull(cryptoUsage.getAlgorithm());
		Assert.assertEquals(
			Collections.emptyList(), cryptoUsage.getConfiguredBy());
		Assert.assertEquals(Primitive.MAC, cryptoUsage.getPrimitive());
		Assert.assertEquals(
			unresolvedReason, cryptoUsage.getUnresolvedReason());
		Assert.assertFalse(cryptoUsage.isResolved());
	}

}