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
import java.util.Set;
import java.util.TreeSet;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoUsageResolverUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetCryptoUsages() {
		String featureName = RandomTestUtil.randomString();

		CryptoCall resolvedCryptoCall = new CryptoCall(
			"com.liferay.test.Class1", featureName, "MessageDigest");

		resolvedCryptoCall.setAlgorithm("SHA-256");
		resolvedCryptoCall.setCryptoFunctions(
			Collections.singletonList(CryptoFunction.DIGEST));

		CryptoCall describedCryptoCall = new CryptoCall(
			"com.liferay.test.Class2", featureName, "Cipher");

		describedCryptoCall.setUnresolvedReason("a method parameter");

		CryptoCall undescribedCryptoCall = new CryptoCall(
			"com.liferay.test.Class3", featureName, "Mac");

		undescribedCryptoCall.setUnresolvedReason(
			"the field com.liferay.test.Class3._algorithm");

		CryptoUsage describedCryptoUsage = new CryptoUsage(
			Arrays.asList("com.liferay.test.Class2", "com.liferay.test.Class4"),
			Collections.singletonList(CryptoFunction.ENCRYPT), null, "Cipher",
			Primitive.UNKNOWN);

		describedCryptoUsage.setConfiguredBy(
			Collections.singletonList("test.algorithm"));

		Set<String> problems = new TreeSet<>();

		List<CryptoUsage> cryptoUsages =
			CryptoUsageResolverUtil.getCryptoUsages(
				Arrays.asList(
					resolvedCryptoCall, describedCryptoCall,
					undescribedCryptoCall),
				Collections.singletonList(describedCryptoUsage), problems);

		Assert.assertEquals(cryptoUsages.toString(), 2, cryptoUsages.size());

		CryptoUsage cryptoUsage = cryptoUsages.get(0);

		Assert.assertEquals("SHA-256", cryptoUsage.getAlgorithm());
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

		cryptoUsage = cryptoUsages.get(1);

		Assert.assertNull(cryptoUsage.getAlgorithm());
		Assert.assertEquals(
			Collections.singletonList("com.liferay.test.Class2"),
			cryptoUsage.getClassNames());
		Assert.assertEquals(
			Collections.singletonList("test.algorithm"),
			cryptoUsage.getConfiguredBy());
		Assert.assertEquals(
			Collections.singletonList(CryptoFunction.ENCRYPT),
			cryptoUsage.getCryptoFunctions());
		Assert.assertEquals(featureName, cryptoUsage.getFeatureName());
		Assert.assertEquals("Cipher", cryptoUsage.getJCAClassName());
		Assert.assertEquals(Primitive.UNKNOWN, cryptoUsage.getPrimitive());

		Assert.assertEquals(
			new TreeSet<>(
				Arrays.asList(
					"Unable to resolve the algorithm of the Mac call in " +
						"com.liferay.test.Class3, which comes from the field " +
							"com.liferay.test.Class3._algorithm",
					"The usage data describes the Cipher call in " +
						"com.liferay.test.Class4, but the scan finds no such " +
							"call whose algorithm it cannot resolve")),
			problems);
	}

}