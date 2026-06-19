/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.pwd;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.exception.PwdEncryptorException;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class PasswordEncryptorUtilFIPSTest {

	@Test
	public void testEncryptNewPasswordRejectsNonApprovedWhenFIPSEnabled() {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			Assert.assertThrows(
				PwdEncryptorException.FIPSAlgorithmNotApproved.class,
				() -> PasswordEncryptorUtil.encrypt("MD5", "test", null));
		}
	}

	@Test
	public void testValidateFIPSAlgorithmAllowsAllWhenFIPSDisabled()
		throws Exception {

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", false)) {

			PasswordEncryptorUtil.validateFIPSAlgorithm("MD5");
		}
	}

	@Test
	public void testValidateFIPSAlgorithmAllowsApprovedWhenFIPSEnabled()
		throws Exception {

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			PasswordEncryptorUtil.validateFIPSAlgorithm(
				"PBKDF2WithHmacSHA256/256/1300000");
			PasswordEncryptorUtil.validateFIPSAlgorithm("SHA-256");
			PasswordEncryptorUtil.validateFIPSAlgorithm("SSHA");
		}
	}

	@Test
	public void testValidateFIPSAlgorithmIgnoresNoneWhenFIPSEnabled()
		throws Exception {

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			PasswordEncryptorUtil.validateFIPSAlgorithm("NONE");
		}
	}

	@Test
	public void testValidateFIPSAlgorithmRejectsNonApprovedWhenFIPSEnabled() {
		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			Assert.assertThrows(
				PwdEncryptorException.FIPSAlgorithmNotApproved.class,
				() -> PasswordEncryptorUtil.validateFIPSAlgorithm("MD5"));
			Assert.assertThrows(
				PwdEncryptorException.FIPSAlgorithmNotApproved.class,
				() -> PasswordEncryptorUtil.validateFIPSAlgorithm("BCRYPT/10"));
		}
	}

}
