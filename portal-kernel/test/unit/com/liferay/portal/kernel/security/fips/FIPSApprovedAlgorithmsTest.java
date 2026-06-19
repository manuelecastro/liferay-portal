/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.fips;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.test.rule.CodeCoverageAssertor;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;

import java.util.Set;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class FIPSApprovedAlgorithmsTest {

	@ClassRule
	public static final CodeCoverageAssertor codeCoverageAssertor =
		CodeCoverageAssertor.INSTANCE;

	@Test
	public void testConstructor() {
		new FIPSApprovedAlgorithms();
	}

	@Test
	public void testGetApprovedAlgorithms() {
		Set<String> digestAlgorithmNames =
			FIPSApprovedAlgorithms.getApprovedAlgorithms(
				FIPSAlgorithmCategory.DIGEST);

		Assert.assertTrue(digestAlgorithmNames.contains("SHA-256"));
		Assert.assertFalse(digestAlgorithmNames.contains("MD5"));
	}

	@Test
	public void testIsApprovedWhenFIPSDisabled() {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", false)) {

			Assert.assertTrue(FIPSApprovedAlgorithms.isApproved("MD5"));
			Assert.assertTrue(FIPSApprovedAlgorithms.isApproved("RSA", 1024));
		}
	}

	@Test
	public void testIsApprovedWhenFIPSEnabled() {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			Assert.assertTrue(FIPSApprovedAlgorithms.isApproved("SHA-256"));
			Assert.assertTrue(
				FIPSApprovedAlgorithms.isApproved("PBKDF2WithHmacSHA256"));
			Assert.assertFalse(FIPSApprovedAlgorithms.isApproved("MD5"));
			Assert.assertFalse(FIPSApprovedAlgorithms.isApproved("MD2"));
		}
	}

	@Test
	public void testIsApprovedWithKeySizeWhenFIPSEnabled() {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			// DIGEST / MAC / SIGNATURE: any key size

			Assert.assertTrue(FIPSApprovedAlgorithms.isApproved("SHA-256", 0));

			// KDF: minimum derived key size

			Assert.assertTrue(
				FIPSApprovedAlgorithms.isApproved("PBKDF2WithHmacSHA256", 160));
			Assert.assertFalse(
				FIPSApprovedAlgorithms.isApproved("PBKDF2WithHmacSHA256", 64));

			// RSA: minimum modulus

			Assert.assertTrue(FIPSApprovedAlgorithms.isApproved("RSA", 2048));
			Assert.assertFalse(FIPSApprovedAlgorithms.isApproved("RSA", 1024));

			// AES: exact key sizes

			Assert.assertTrue(FIPSApprovedAlgorithms.isApproved("AES", 256));
			Assert.assertFalse(FIPSApprovedAlgorithms.isApproved("AES", 64));

			// EC: allowed curves

			Assert.assertTrue(FIPSApprovedAlgorithms.isApproved("EC", 256));
			Assert.assertFalse(FIPSApprovedAlgorithms.isApproved("EC", 192));

			// Unknown algorithm with a key size

			Assert.assertFalse(FIPSApprovedAlgorithms.isApproved("MD5", 256));
		}
	}

}
