/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
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
		CryptoUsage cipherCryptoUsage = new CryptoUsage(
			Collections.singletonList("com.liferay.test.Class1"),
			Arrays.asList(CryptoFunction.ENCRYPT, CryptoFunction.DECRYPT),
			"Feature 1", "Cipher", Primitive.AE);

		cipherCryptoUsage.setAlgorithm("AES/GCM/NoPadding");
		cipherCryptoUsage.setKeySize(256);

		CryptoUsage keyGeneratorCryptoUsage = new CryptoUsage(
			Collections.singletonList("com.liferay.test.Class2"),
			Collections.singletonList(CryptoFunction.KEYGEN), "Feature 2",
			"KeyGenerator", Primitive.BLOCK_CIPHER);

		keyGeneratorCryptoUsage.setConfiguredBy(
			Arrays.asList("test.encryption.algorithm", "test.key.size"));

		CryptoUsage messageDigestCryptoUsage1 = new CryptoUsage(
			Collections.singletonList("com.liferay.test.Class3"),
			Collections.singletonList(CryptoFunction.DIGEST), "Feature 3",
			"MessageDigest", Primitive.HASH);

		messageDigestCryptoUsage1.setAlgorithm("SHA-256");

		CryptoUsage messageDigestCryptoUsage2 = new CryptoUsage(
			Arrays.asList("com.liferay.test.Class4", "com.liferay.test.Class3"),
			Collections.singletonList(CryptoFunction.DIGEST), "Feature 4",
			"MessageDigest", Primitive.HASH);

		messageDigestCryptoUsage2.setAlgorithm("SHA-256");

		CryptoUsage cipherConfigurableCryptoUsage = new CryptoUsage(
			Collections.singletonList("com.liferay.test.Class5"),
			Collections.singletonList(CryptoFunction.ENCRYPT), "Feature 2",
			"Cipher", Primitive.UNKNOWN);

		cipherConfigurableCryptoUsage.setConfiguredBy(
			Arrays.asList("test.encryption.algorithm", "test.key.size"));

		List<Component> components = CryptoAssetComponentUtil.toComponents(
			Arrays.asList(
				messageDigestCryptoUsage1, keyGeneratorCryptoUsage,
				cipherCryptoUsage, messageDigestCryptoUsage2,
				cipherConfigurableCryptoUsage));

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"bom-ref", "crypto:Cipher:AES/GCM/NoPadding:256"
				).put(
					"cryptoProperties",
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
					)
				).put(
					"evidence",
					JSONUtil.put(
						"occurrences",
						JSONUtil.putAll(
							JSONUtil.put(
								"location", "com.liferay.test.Class1")))
				).put(
					"name", "AES/GCM/NoPadding"
				).put(
					"properties",
					JSONUtil.putAll(
						JSONUtil.put(
							"name", "liferay:cbom:feature"
						).put(
							"value", "Feature 1"
						),
						JSONUtil.put(
							"name", "liferay:cbom:jca-class"
						).put(
							"value", "Cipher"
						),
						JSONUtil.put(
							"name", "liferay:cbom:provider"
						).put(
							"value", "SunJCE"
						))
				).put(
					"type", "cryptographic-asset"
				),
				JSONUtil.put(
					"bom-ref", "crypto:MessageDigest:SHA-256"
				).put(
					"cryptoProperties",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("digest")
						).put(
							"primitive", "hash"
						)
					).put(
						"assetType", "algorithm"
					)
				).put(
					"evidence",
					JSONUtil.put(
						"occurrences",
						JSONUtil.putAll(
							JSONUtil.put("location", "com.liferay.test.Class3"),
							JSONUtil.put(
								"location", "com.liferay.test.Class4")))
				).put(
					"name", "SHA-256"
				).put(
					"properties",
					JSONUtil.putAll(
						JSONUtil.put(
							"name", "liferay:cbom:feature"
						).put(
							"value", "Feature 3"
						),
						JSONUtil.put(
							"name", "liferay:cbom:feature"
						).put(
							"value", "Feature 4"
						),
						JSONUtil.put(
							"name", "liferay:cbom:jca-class"
						).put(
							"value", "MessageDigest"
						),
						JSONUtil.put(
							"name", "liferay:cbom:provider"
						).put(
							"value", "SUN"
						))
				).put(
					"type", "cryptographic-asset"
				),
				JSONUtil.put(
					"bom-ref", "crypto:test.encryption.algorithm"
				).put(
					"cryptoProperties",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions",
							JSONUtil.putAll("keygen", "encrypt")
						).put(
							"primitive", "unknown"
						)
					).put(
						"assetType", "algorithm"
					)
				).put(
					"evidence",
					JSONUtil.put(
						"occurrences",
						JSONUtil.putAll(
							JSONUtil.put("location", "com.liferay.test.Class2"),
							JSONUtil.put(
								"location", "com.liferay.test.Class5")))
				).put(
					"name", "test.encryption.algorithm"
				).put(
					"properties",
					JSONUtil.putAll(
						JSONUtil.put(
							"name", "liferay:cbom:configured-by"
						).put(
							"value", "test.encryption.algorithm"
						),
						JSONUtil.put(
							"name", "liferay:cbom:configured-by"
						).put(
							"value", "test.key.size"
						),
						JSONUtil.put(
							"name", "liferay:cbom:feature"
						).put(
							"value", "Feature 2"
						),
						JSONUtil.put(
							"name", "liferay:cbom:jca-class"
						).put(
							"value", "Cipher"
						),
						JSONUtil.put(
							"name", "liferay:cbom:jca-class"
						).put(
							"value", "KeyGenerator"
						))
				).put(
					"type", "cryptographic-asset"
				)
			).toString(),
			_toJSON(components), JSONCompareMode.STRICT);
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

}