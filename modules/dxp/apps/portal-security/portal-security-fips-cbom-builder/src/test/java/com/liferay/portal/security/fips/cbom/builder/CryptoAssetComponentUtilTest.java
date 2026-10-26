/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;
import org.cyclonedx.model.component.crypto.enums.RelatedCryptoMaterialType;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Manuele Castro
 */
public class CryptoAssetComponentUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		jsonFactoryUtil.setJSONFactory(new JSONFactoryImpl());
	}

	@Test
	public void testToComponents() throws Exception {
		CryptoUsage cipherCryptoUsage = _createCryptoUsage(
			"AES/GCM/NoPadding",
			Arrays.asList(CryptoFunction.ENCRYPT, CryptoFunction.DECRYPT),
			"Cipher", Primitive.AE, "com.liferay.test.Class1");

		cipherCryptoUsage.setKeySize(256);

		CryptoUsage keyGeneratorCryptoUsage = _createCryptoUsage(
			null, Collections.singletonList(CryptoFunction.KEYGEN),
			"KeyGenerator", Primitive.BLOCK_CIPHER, "com.liferay.test.Class2");

		keyGeneratorCryptoUsage.setConfiguredBy(
			Arrays.asList("test.encryption.algorithm", "test.key.size"));

		CryptoUsage cipherConfigurableCryptoUsage = _createCryptoUsage(
			null, Collections.singletonList(CryptoFunction.ENCRYPT), "Cipher",
			Primitive.UNKNOWN, "com.liferay.test.Class2");

		cipherConfigurableCryptoUsage.setConfiguredBy(
			Arrays.asList("test.encryption.algorithm", "test.key.size"));

		CryptoUsage keyFactoryCryptoUsage = _createCryptoUsage(
			"DSA", Collections.singletonList(CryptoFunction.OTHER),
			"KeyFactory", Primitive.UNKNOWN, "com.liferay.test.Class3");

		keyFactoryCryptoUsage.setRelatedCryptoMaterialTypes(
			Collections.singletonList(RelatedCryptoMaterialType.PUBLIC_KEY));

		CryptoUsage unresolvedKeyStoreCryptoUsage = _createCryptoUsage(
			null, Collections.singletonList(CryptoFunction.OTHER), "KeyStore",
			Primitive.OTHER, "com.liferay.test.Class10");

		unresolvedKeyStoreCryptoUsage.setUnresolvedReason(
			"the return value of com.liferay.test.Class10.getKeyStoreType()");

		CryptoUsage unresolvedMessageDigestCryptoUsage = _createCryptoUsage(
			null, Collections.singletonList(CryptoFunction.DIGEST),
			"MessageDigest", Primitive.HASH, "com.liferay.test.Class9");

		unresolvedMessageDigestCryptoUsage.setUnresolvedReason(
			"parameter 1 of com.liferay.test.Class9.digest");

		List<Component> components = CryptoAssetComponentUtil.toComponents(
			Arrays.asList(
				_createCryptoUsage(
					"SHA-256", Collections.singletonList(CryptoFunction.DIGEST),
					"MessageDigest", Primitive.HASH, "com.liferay.test.Class4"),
				keyGeneratorCryptoUsage, cipherCryptoUsage,
				_createCryptoUsage(
					"SHA-256", Collections.singletonList(CryptoFunction.DIGEST),
					"MessageDigest", Primitive.HASH, "com.liferay.test.Class5",
					"com.liferay.test.Class4"),
				cipherConfigurableCryptoUsage, keyFactoryCryptoUsage,
				_createCryptoUsage(
					"DSA", Collections.singletonList(CryptoFunction.VERIFY),
					"Signature", Primitive.SIGNATURE,
					"com.liferay.test.Class3"),
				_createCryptoUsage(
					"HmacSHA1", Collections.singletonList(CryptoFunction.TAG),
					"Mac", Primitive.MAC, "com.liferay.test.Class6"),
				_createCryptoUsage(
					"RAW", Collections.singletonList(CryptoFunction.OTHER),
					"SecretKeySpec", Primitive.UNKNOWN,
					"com.liferay.test.Class6"),
				_createCryptoUsage(
					"pkcs12", Collections.singletonList(CryptoFunction.OTHER),
					"KeyStore", Primitive.OTHER, "com.liferay.test.Class7"),
				_createCryptoUsage(
					"TLSv1.2", Collections.singletonList(CryptoFunction.OTHER),
					"SSLContext", Primitive.OTHER, "com.liferay.test.Class8"),
				unresolvedKeyStoreCryptoUsage,
				unresolvedMessageDigestCryptoUsage));

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				_toJSONObject(
					"crypto:Cipher:AES/GCM/NoPadding:256",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions",
							JSONUtil.putAll("encrypt", "decrypt")
						).put(
							"mode", "gcm"
						).put(
							"parameterSetIdentifier", "256"
						).put(
							"primitive", "ae"
						)
					).put(
						"assetType", "algorithm"
					),
					"Cipher", "AES-256-GCM", "com.liferay.test.Class1"),
				_toJSONObject(
					"crypto:Cipher:test.encryption.algorithm",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("encrypt")
						).put(
							"primitive", "unknown"
						)
					).put(
						"assetType", "algorithm"
					),
					"Cipher", "test.encryption.algorithm",
					"com.liferay.test.Class2"),
				_toJSONObject(
					"crypto:KeyFactory:DSA",
					JSONUtil.put(
						"assetType", "related-crypto-material"
					).put(
						"relatedCryptoMaterialProperties",
						JSONUtil.put(
							"algorithmRef", "crypto:Signature:SHA1withDSA"
						).put(
							"type", "public-key"
						)
					),
					"KeyFactory", "DSA", "com.liferay.test.Class3"),
				_toJSONObject(
					"crypto:KeyGenerator:test.encryption.algorithm",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("keygen")
						).put(
							"primitive", "block-cipher"
						)
					).put(
						"assetType", "algorithm"
					),
					"KeyGenerator", "test.encryption.algorithm",
					"com.liferay.test.Class2"),
				_toJSONObject(
					"crypto:KeyStore:PKCS12",
					JSONUtil.put(
						"assetType", "related-crypto-material"
					).put(
						"relatedCryptoMaterialProperties",
						JSONUtil.put(
							"format", "PKCS12"
						).put(
							"type", "other"
						)
					),
					"KeyStore", "PKCS12", "com.liferay.test.Class7"),
				_toJSONObject(
					"crypto:KeyStore:com.liferay.test.Class10",
					JSONUtil.put(
						"assetType", "related-crypto-material"
					).put(
						"relatedCryptoMaterialProperties",
						JSONUtil.put("type", "other")
					),
					"KeyStore", "KeyStore", "com.liferay.test.Class10"
				).put(
					"description",
					"The scan cannot read the algorithm, which comes from " +
						"the return value of " +
							"com.liferay.test.Class10.getKeyStoreType()."
				),
				_toJSONObject(
					"crypto:Mac:HmacSHA1",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("tag")
						).put(
							"primitive", "mac"
						)
					).put(
						"assetType", "algorithm"
					),
					"Mac", "HMAC-SHA-1", "com.liferay.test.Class6"),
				_toJSONObject(
					"crypto:MessageDigest:SHA-256",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("digest")
						).put(
							"primitive", "hash"
						)
					).put(
						"assetType", "algorithm"
					),
					"MessageDigest", "SHA-256", "com.liferay.test.Class4",
					"com.liferay.test.Class5"),
				_toJSONObject(
					"crypto:MessageDigest:com.liferay.test.Class9",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("digest")
						).put(
							"primitive", "hash"
						)
					).put(
						"assetType", "algorithm"
					),
					"MessageDigest", "MessageDigest", "com.liferay.test.Class9"
				).put(
					"description",
					"The scan cannot read the algorithm, which comes from " +
						"parameter 1 of com.liferay.test.Class9.digest."
				),
				_toJSONObject(
					"crypto:SSLContext:TLSv1.2",
					JSONUtil.put(
						"assetType", "protocol"
					).put(
						"protocolProperties",
						JSONUtil.put(
							"type", "tls"
						).put(
							"version", "1.2"
						)
					),
					"SSLContext", "TLSv1.2", "com.liferay.test.Class8"),
				_toJSONObject(
					"crypto:SecretKeySpec:RAW",
					JSONUtil.put(
						"assetType", "related-crypto-material"
					).put(
						"relatedCryptoMaterialProperties",
						JSONUtil.put(
							"algorithmRef", "crypto:Mac:HmacSHA1"
						).put(
							"type", "secret-key"
						)
					),
					"SecretKeySpec", "RAW", "com.liferay.test.Class6"),
				_toJSONObject(
					"crypto:Signature:SHA1withDSA",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("verify")
						).put(
							"primitive", "signature"
						)
					).put(
						"assetType", "algorithm"
					),
					"Signature", "DSA-SHA-1", "com.liferay.test.Class3")
			).toString(),
			_toJSON(components), JSONCompareMode.STRICT);
	}

	private CryptoUsage _createCryptoUsage(
		String algorithm, List<CryptoFunction> cryptoFunctions,
		String jcaClassName, Primitive primitive, String... classNames) {

		CryptoUsage cryptoUsage = new CryptoUsage(
			RandomTestUtil.randomString(), Arrays.asList(classNames),
			cryptoFunctions, RandomTestUtil.randomString(), jcaClassName,
			primitive);

		cryptoUsage.setAlgorithm(algorithm);

		return cryptoUsage;
	}

	private String _toJSON(List<Component> components) throws Exception {
		Bom bom = new Bom();

		bom.setComponents(components);

		BomJsonGenerator bomJsonGenerator = BomGeneratorFactory.createJson(
			Version.VERSION_16, bom);

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			bomJsonGenerator.toJsonString());

		return String.valueOf(jsonObject.getJSONArray("components"));
	}

	private JSONObject _toJSONObject(
			String bomRef, JSONObject cryptoPropertiesJSONObject,
			String jcaClassName, String name, String... locations)
		throws Exception {

		JSONArray occurrencesJSONArray = JSONUtil.toJSONArray(
			Arrays.asList(locations),
			location -> JSONUtil.put("location", location));

		return JSONUtil.put(
			"bom-ref", bomRef
		).put(
			"cryptoProperties", cryptoPropertiesJSONObject
		).put(
			"evidence", JSONUtil.put("occurrences", occurrencesJSONArray)
		).put(
			"name", name
		).put(
			"properties",
			JSONUtil.putAll(
				JSONUtil.put(
					"name", "liferay:cbom:jca-class"
				).put(
					"value", jcaClassName
				))
		).put(
			"type", "cryptographic-asset"
		);
	}

}