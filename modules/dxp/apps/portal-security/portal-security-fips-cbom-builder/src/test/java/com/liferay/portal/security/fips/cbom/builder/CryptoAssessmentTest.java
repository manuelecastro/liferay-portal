/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.time.LocalDate;

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
	public void testFetchInactiveCertificateMessage() throws Exception {
		CryptoAssessment cryptoAssessment = CryptoAssessment.load(
			"BCFIPS", "2.1.1");

		Assert.assertNull(
			cryptoAssessment.fetchInactiveCertificateMessage(
				LocalDate.of(2030, 1, 16)));
		Assert.assertEquals(
			"The CMVP certificate 4943 of BC-FJA (Bouncy Castle FIPS Java " +
				"API) passed its sunset date 2030-01-16",
			cryptoAssessment.fetchInactiveCertificateMessage(
				LocalDate.of(2030, 1, 17)));

		ObjectMapper objectMapper = new ObjectMapper();

		JsonNode jsonNode = objectMapper.readTree(
			CryptoAssessment.class.getResourceAsStream(
				"dependencies/cbom-reference-data.json"));

		ObjectNode objectNode = (ObjectNode)jsonNode.at(
			"/modules/0/certificate");

		String status = RandomTestUtil.randomString();

		objectNode.put("status", status);

		cryptoAssessment = new CryptoAssessment("BCFIPS", "2.1.1", jsonNode);

		Assert.assertEquals(
			StringBundler.concat(
				"The CMVP certificate 4943 of BC-FJA (Bouncy Castle FIPS Java ",
				"API) is ", status, ", not Active"),
			cryptoAssessment.fetchInactiveCertificateMessage(
				LocalDate.of(2030, 1, 16)));

		cryptoAssessment = CryptoAssessment.load(null, null);

		Assert.assertNull(
			cryptoAssessment.fetchInactiveCertificateMessage(
				LocalDate.of(2030, 1, 17)));
	}

	@Test
	public void testFetchNISTQuantumSecurityLevel() throws Exception {
		CryptoAssessment cryptoAssessment = CryptoAssessment.load(null, null);

		Assert.assertEquals(
			Integer.valueOf(0),
			cryptoAssessment.fetchNISTQuantumSecurityLevel(
				"ECDSA-P-256-SHA-256"));
		Assert.assertEquals(
			Integer.valueOf(0),
			cryptoAssessment.fetchNISTQuantumSecurityLevel(
				"RSA-PKCS1-1.5-SHA-256"));
		Assert.assertEquals(
			Integer.valueOf(1),
			cryptoAssessment.fetchNISTQuantumSecurityLevel("AES-128-GCM"));
		Assert.assertEquals(
			Integer.valueOf(5),
			cryptoAssessment.fetchNISTQuantumSecurityLevel("AES-256"));
		Assert.assertNull(
			cryptoAssessment.fetchNISTQuantumSecurityLevel("AES-GCM"));
		Assert.assertNull(
			cryptoAssessment.fetchNISTQuantumSecurityLevel("HMAC-SHA-256"));
	}

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
	public void testGetCurveStatus() throws Exception {
		CryptoAssessment cryptoAssessment = CryptoAssessment.load(null, null);

		Assert.assertEquals(
			"approved-for-ecdsa-and-ec-key-establishment",
			cryptoAssessment.getCurveStatus("P-256"));
		Assert.assertEquals(
			"approved-for-eddsa",
			cryptoAssessment.getCurveStatus("Edwards25519"));
		Assert.assertEquals(
			"blockchain-only", cryptoAssessment.getCurveStatus("secp256k1"));
		Assert.assertEquals(
			"not-for-direct-use",
			cryptoAssessment.getCurveStatus("Curve25519"));
		Assert.assertEquals(
			"not-listed",
			cryptoAssessment.getCurveStatus(RandomTestUtil.randomString()));
		Assert.assertEquals("unknown", cryptoAssessment.getCurveStatus(null));
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

	@Test
	public void testIsNISTApprovedCurve() throws Exception {
		CryptoAssessment cryptoAssessment = CryptoAssessment.load(null, null);

		Assert.assertTrue(
			cryptoAssessment.isNISTApprovedCurve("Edwards448", "EdDSA"));
		Assert.assertTrue(
			cryptoAssessment.isNISTApprovedCurve("P-384", "ECDH"));
		Assert.assertFalse(
			cryptoAssessment.isNISTApprovedCurve("Curve25519", "XDH"));
		Assert.assertFalse(
			cryptoAssessment.isNISTApprovedCurve("P-256", "EdDSA"));
		Assert.assertFalse(
			cryptoAssessment.isNISTApprovedCurve("secp256k1", "ECDSA"));
	}

}