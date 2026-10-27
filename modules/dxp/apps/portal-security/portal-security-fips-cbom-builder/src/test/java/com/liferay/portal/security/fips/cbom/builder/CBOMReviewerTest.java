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
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CBOMReviewerTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetCertificates() throws Exception {
		Assert.assertEquals(
			Collections.singleton(
				StringBundler.concat(
					"BCFIPS 2.1.2: CMVP certificate 4943, Active, sunset date ",
					"2030-01-16, https://csrc.nist.gov/projects/cryptographic-",
					"module-validation-program/certificate/4943")),
			CBOMReviewer.getCertificates(_readTree("current.cdx.json")));
	}

	@Test
	public void testReview() throws Exception {
		JsonNode previousJsonNode = _readTree("previous.cdx.json");

		ObjectNode objectNode = previousJsonNode.deepCopy();

		objectNode.put("serialNumber", RandomTestUtil.randomString());
		objectNode.put("version", RandomTestUtil.randomInt());

		ObjectNode componentObjectNode = (ObjectNode)objectNode.at(
			"/metadata/component");

		componentObjectNode.put("version", RandomTestUtil.randomString());

		Assert.assertEquals(
			Collections.emptyMap(),
			CBOMReviewer.review(objectNode, previousJsonNode));

		Map<String, Set<String>> changes =
			LinkedHashMapBuilder.<String, Set<String>>put(
				"New algorithms",
				SetUtil.fromArray(
					"HMAC-SHA-256 (crypto:Mac:HmacSHA256): APPROVED, " +
						"provider BCFIPS",
					"XDH-X25519 (crypto:KeyAgreement:X25519): APPROVED, " +
						"provider BCFIPS")
			).put(
				"Changed providers",
				SetUtil.fromArray(
					"AES-GCM (crypto:Cipher:AES/GCM/NoPadding): from BCFIPS " +
						"to SunJCE",
					"Adds SunJCE (provider:SunJCE)",
					"BCFIPS (provider:BCFIPS): version from 2.1.1 to 2.1.2")
			).put(
				"Changed approval statuses",
				SetUtil.fromArray(
					"AES-GCM (crypto:Cipher:AES/GCM/NoPadding): from " +
						"APPROVED to PROVIDER_NOT_VALIDATED")
			).put(
				"Newly flagged curves",
				SetUtil.fromArray(
					"ECDSA-secp256k1-SHA-256 " +
						"(crypto:Signature:SHA256withECDSA:secp256k1)",
					"XDH-X25519 (crypto:KeyAgreement:X25519)")
			).put(
				"New configurable entries",
				SetUtil.fromArray(
					"keystore.type (crypto:KeyStore:keystore.type)",
					"keystore.type (setting:keystore.type)")
			).put(
				"TLS list changes",
				SetUtil.fromArray(
					"TLSv1.2 (crypto:SSLContext:TLSv1.2): adds " +
						"[TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384], removes " +
							"[TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256]")
			).put(
				"New uses the scan cannot resolve",
				SetUtil.fromArray(
					StringBundler.concat(
						"MessageDigest (crypto:MessageDigest:com.liferay.foo.",
						"Bar): The scan cannot read the algorithm, which ",
						"comes from parameter 1 of Bar.digest."))
			).put(
				"Changed uses",
				SetUtil.fromArray(
					"module:com.liferay.foo no longer uses " +
						"crypto:MessageDigest:MD5",
					"module:com.liferay.foo uses crypto:KeyStore:keystore.type",
					"module:com.liferay.foo uses crypto:Mac:HmacSHA256",
					"module:com.liferay.foo uses " +
						"crypto:MessageDigest:com.liferay.foo.Bar")
			).put(
				"Removed entries",
				SetUtil.fromArray("MD5 (crypto:MessageDigest:MD5)")
			).put(
				"Changed entries",
				SetUtil.fromArray(
					"AES-GCM (crypto:Cipher:AES/GCM/NoPadding): properties " +
						"(liferay:cbom:fips-approval-status)",
					"BCFIPS (provider:BCFIPS): version",
					"ECDSA-secp256k1-SHA-256 " +
						"(crypto:Signature:SHA256withECDSA:secp256k1): " +
							"properties (liferay:cbom:non-nist-approved-curve)",
					"TLSv1.2 (crypto:SSLContext:TLSv1.2): cryptoProperties")
			).put(
				"Changed metadata",
				SetUtil.fromArray(
					"liferay:cbom:fips-provider: from BCFIPS 2.1.1 to BCFIPS " +
						"2.1.2")
			).build();

		Assert.assertEquals(
			new ArrayList<>(changes.entrySet()),
			new ArrayList<>(
				CBOMReviewer.review(
					_readTree("current.cdx.json"), previousJsonNode
				).entrySet()));
	}

	private JsonNode _readTree(String fileName) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		return objectMapper.readTree(
			CBOMReviewerTest.class.getResourceAsStream(
				"dependencies/" + fileName));
	}

}