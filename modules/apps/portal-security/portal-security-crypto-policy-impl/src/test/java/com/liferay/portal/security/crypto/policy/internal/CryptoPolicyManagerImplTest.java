/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.internal;

import com.liferay.portal.security.crypto.policy.ServiceType;
import com.liferay.portal.security.crypto.policy.exception.CryptoPolicyException;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.security.Provider;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoPolicyManagerImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_cryptoPolicyManagerImpl = new CryptoPolicyManagerImpl();

		_cryptoPolicyManagerImpl.buildAlgorithmMap(
			new Provider[] {new MockFIPSProvider()});
		_cryptoPolicyManagerImpl.buildKeySizeMap(
			new Provider[] {new MockFIPSProvider()});
	}

	@Test
	public void testCheckAlgorithmFIPSApprovedReturnsAlgorithm() {
		CryptoPolicyManagerImpl cryptoPolicyManagerImpl = new FIPSEnabledImpl();

		cryptoPolicyManagerImpl.buildAlgorithmMap(
			new Provider[] {new MockFIPSProvider()});
		cryptoPolicyManagerImpl.buildKeySizeMap(
			new Provider[] {new MockFIPSProvider()});

		String result = cryptoPolicyManagerImpl.checkAlgorithm(
			"SHA-256", ServiceType.MESSAGE_DIGEST);

		Assert.assertEquals("SHA-256", result);
	}

	@Test(expected = CryptoPolicyException.class)
	public void testCheckAlgorithmFIPSUnapprovedThrows() {
		CryptoPolicyManagerImpl cryptoPolicyManagerImpl = new FIPSEnabledImpl();

		cryptoPolicyManagerImpl.buildAlgorithmMap(
			new Provider[] {new MockFIPSProvider()});
		cryptoPolicyManagerImpl.buildKeySizeMap(
			new Provider[] {new MockFIPSProvider()});

		cryptoPolicyManagerImpl.checkAlgorithm(
			"MD5", ServiceType.MESSAGE_DIGEST);
	}

	@Test
	public void testCheckAlgorithmWithKeySizeWithoutFIPSAlwaysReturnsAlgorithm() {
		String result = _cryptoPolicyManagerImpl.checkAlgorithm(
			"AES", 192, ServiceType.KEY_GENERATOR);

		Assert.assertEquals("AES", result);
	}

	@Test
	public void testCheckAlgorithmWithoutFIPSAlwaysReturnsAlgorithm() {
		String result = _cryptoPolicyManagerImpl.checkAlgorithm(
			"MD5", ServiceType.MESSAGE_DIGEST);

		Assert.assertEquals("MD5", result);
	}

	@Test
	public void testGetAllowedAlgorithmsReturnsEmptySetForUnknownServiceType() {
		Set<String> algorithms = _cryptoPolicyManagerImpl.getAllowedAlgorithms(
			ServiceType.MAC);

		Assert.assertTrue(algorithms.isEmpty());
	}

	@Test
	public void testGetAllowedAlgorithmsReturnsOnlyExposedAlgorithms() {
		Set<String> algorithms = _cryptoPolicyManagerImpl.getAllowedAlgorithms(
			ServiceType.MESSAGE_DIGEST);

		Assert.assertTrue(algorithms.contains("SHA-256"));
		Assert.assertFalse(algorithms.contains("MD5"));
		Assert.assertFalse(algorithms.contains("SHA-1"));
	}

	private CryptoPolicyManagerImpl _cryptoPolicyManagerImpl;

	private static class FIPSEnabledImpl extends CryptoPolicyManagerImpl {

		@Override
		protected boolean isFIPSEnabled() {
			return true;
		}

	}

	private static class MockFIPSProvider extends Provider {

		private MockFIPSProvider() {
			super("MockFIPS", "1.0", "Mock FIPS provider for testing");

			putService(
				new Service(
					this, "MessageDigest", "SHA-256", MockImpl.class.getName(),
					null, null));
			putService(
				new Service(
					this, "Cipher", "AES", MockImpl.class.getName(), null,
					null));
			putService(
				new Service(
					this, "KeyGenerator", "AES", MockImpl.class.getName(), null,
					null));
		}

		private static class MockImpl {
		}

	}

}