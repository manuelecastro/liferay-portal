/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;

import org.cyclonedx.model.component.crypto.enums.CertificationLevel;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoAssessmentTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetCertificationLevel() throws Exception {
		CryptoAssessment cryptoAssessment = CryptoAssessment.load(
			"BCFIPS", "2.1.1");

		Assert.assertEquals(
			CertificationLevel.FIPS140_3_L1,
			cryptoAssessment.getCertificationLevel("APPROVED"));
		Assert.assertEquals(
			CertificationLevel.FIPS140_3_L1,
			cryptoAssessment.getCertificationLevel("VENDOR_AFFIRMED"));
		Assert.assertEquals(
			CertificationLevel.NONE,
			cryptoAssessment.getCertificationLevel("NON_APPROVED_NOT_ALLOWED"));

		cryptoAssessment = CryptoAssessment.load(null, null);

		Assert.assertEquals(
			CertificationLevel.NONE,
			cryptoAssessment.getCertificationLevel("APPROVED"));
	}

	@Test
	public void testGetFIPSApprovalStatus() throws Exception {
		CryptoAssessment cryptoAssessment = CryptoAssessment.load(
			"BCFIPS", "2.1.1");

		Assert.assertTrue(cryptoAssessment.hasFIPSModule());

		Assert.assertEquals(
			"APPROVED",
			cryptoAssessment.getFIPSApprovalStatus(
				"Cipher", "AES-256-GCM", "BCFIPS"));
		Assert.assertEquals(
			"APPROVED",
			cryptoAssessment.getFIPSApprovalStatus(
				"SecretKeySpec", "AES-128", null));
		Assert.assertEquals(
			"APPROVED",
			cryptoAssessment.getFIPSApprovalStatus(
				"Signature", "RSA-PKCS1-1.5-SHA-256", "BCFIPS"));
		Assert.assertEquals(
			"NON_APPROVED_NOT_ALLOWED",
			cryptoAssessment.getFIPSApprovalStatus(
				"Cipher", "3DES-CBC", "BCFIPS"));
		Assert.assertEquals(
			"NON_APPROVED_NOT_ALLOWED",
			cryptoAssessment.getFIPSApprovalStatus(
				"Cipher", "RSA-PKCS1-1.5", "BCFIPS"));
		Assert.assertEquals(
			"PROVIDER_NOT_VALIDATED",
			cryptoAssessment.getFIPSApprovalStatus(
				"MessageDigest", "MD5", "SUN"));
		Assert.assertEquals(
			"UNKNOWN",
			cryptoAssessment.getFIPSApprovalStatus(
				"SecretKeySpec", "RAW", null));
		Assert.assertEquals(
			"VENDOR_AFFIRMED",
			cryptoAssessment.getFIPSApprovalStatus(
				"KeyGenerator", "AES-256", "BCFIPS"));

		cryptoAssessment = CryptoAssessment.load("BCFIPS", "2.1.3");

		Assert.assertFalse(cryptoAssessment.hasFIPSModule());

		Assert.assertEquals(
			"PROVIDER_NOT_VALIDATED",
			cryptoAssessment.getFIPSApprovalStatus(
				"Cipher", "AES-256-GCM", "BCFIPS"));
	}

	@Test
	public void testGetModuleProperties() throws Exception {
		CryptoAssessment cryptoAssessment = CryptoAssessment.load(
			"BCFIPS", "2.1.1");

		Assert.assertEquals(
			HashMapBuilder.put(
				"liferay:cbom:cmvp-certificate", "4943"
			).put(
				"liferay:cbom:cmvp-status", "Active"
			).put(
				"liferay:cbom:cmvp-sunset-date", "2030-01-16"
			).put(
				"liferay:cbom:fips-module",
				"BC-FJA (Bouncy Castle FIPS Java API)"
			).build(),
			cryptoAssessment.getModuleProperties());
		Assert.assertEquals(
			"https://csrc.nist.gov/projects/cryptographic-module-validation-" +
				"program/certificate/4943",
			cryptoAssessment.fetchCertificateURL());
		Assert.assertEquals(
			Arrays.asList(
				"CKG using output from the DRBG (SP 800-133, Section 4), per " +
					"FIPS 140-3 IG D.H"),
			cryptoAssessment.getVendorAffirmedMethods());
	}

}