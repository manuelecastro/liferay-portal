/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.security.fips.FIPSModeValidator;
import com.liferay.portal.kernel.security.xml.SecureXMLFactoryProviderUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.io.File;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class CBOMTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule());

	public static void assume() {
		Assume.assumeTrue(
			"Directory \"" + _cbomDir + "\" does not exist", _cbomDir.exists());
	}

	@Before
	public void setUp() throws Exception {
		String fipsProviderName = null;

		if (PropsValues.FIPS_ENABLED) {
			Provider provider = FIPSModeValidator.fetchProvider();

			fipsProviderName = provider.getName();
		}

		for (File file : _cbomDir.listFiles()) {
			String fileName = file.getName();

			if (!fileName.endsWith(".cdx.json")) {
				continue;
			}

			JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
				FileUtil.read(file));

			if (!Objects.equals(
					_fetchFIPSProviderName(jsonObject), fipsProviderName)) {

				continue;
			}

			_cbomJSONObject = jsonObject;
		}

		Assert.assertNotNull(_cbomJSONObject);
	}

	@Test
	public void testClusterCiphers() throws Exception {
		Assume.assumeTrue(PropsValues.FIPS_ENABLED);

		DocumentBuilderFactory documentBuilderFactory =
			SecureXMLFactoryProviderUtil.newDocumentBuilderFactory();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.newDocument();

		Set<Integer> keySizes = new TreeSet<>();

		for (String bomRef :
				_getDependsOn("module:com.liferay.portal.cluster.multiple")) {

			if (!bomRef.startsWith("crypto:Cipher:")) {
				continue;
			}

			JSONObject componentJSONObject = _getComponentJSONObject(bomRef);

			JSONObject cryptoPropertiesJSONObject =
				componentJSONObject.getJSONObject("cryptoProperties");

			JSONObject algorithmPropertiesJSONObject =
				cryptoPropertiesJSONObject.getJSONObject("algorithmProperties");

			if (!algorithmPropertiesJSONObject.has("parameterSetIdentifier")) {
				continue;
			}

			String[] parts = StringUtil.split(bomRef, CharPool.COLON);

			Element element = document.createElement("SYM_ENCRYPT");

			element.setAttribute("sym_algorithm", parts[2]);
			element.setAttribute("sym_iv_length", "16");
			element.setAttribute(
				"sym_keylength",
				algorithmPropertiesJSONObject.getString(
					"parameterSetIdentifier"));

			ReflectionTestUtil.invoke(
				FIPSModeValidator.class,
				"_validateClusterLinkChannelSymEncryptElement",
				new Class<?>[] {Element.class}, element);

			keySizes.add(
				algorithmPropertiesJSONObject.getInt("parameterSetIdentifier"));
		}

		Assert.assertEquals(
			ReflectionTestUtil.getFieldValue(
				FIPSModeValidator.class, "_allowedSymmetricKeySizes"),
			keySizes);
	}

	@Test
	public void testFIPSProvider() throws Exception {
		Assume.assumeTrue(PropsValues.FIPS_ENABLED);

		Provider provider = FIPSModeValidator.fetchProvider();

		JSONObject componentJSONObject = _getComponentJSONObject(
			"provider:" + provider.getName());

		Assert.assertEquals(
			provider.getName(), componentJSONObject.getString("name"));
		Assert.assertEquals(
			_getVersion(provider), componentJSONObject.getString("version"));
	}

	@Test
	public void testProviders() throws Exception {
		Map<String, String> actualProviderNames = new TreeMap<>();
		Map<String, String> expectedProviderNames = new TreeMap<>();

		JSONArray dependenciesJSONArray = _cbomJSONObject.getJSONArray(
			"dependencies");

		for (int i = 0; i < dependenciesJSONArray.length(); i++) {
			JSONObject dependencyJSONObject =
				dependenciesJSONArray.getJSONObject(i);

			String ref = dependencyJSONObject.getString("ref");

			if (!ref.startsWith("provider:")) {
				continue;
			}

			for (String bomRef :
					JSONUtil.toStringList(
						dependencyJSONObject.getJSONArray("provides"))) {

				String[] parts = StringUtil.split(bomRef, CharPool.COLON);

				Provider provider = _getProvider(parts[2], parts[1]);

				actualProviderNames.put(bomRef, provider.getName());

				expectedProviderNames.put(
					bomRef, StringUtil.removeSubstring(ref, "provider:"));
			}
		}

		Assert.assertEquals(expectedProviderNames, actualProviderNames);
	}

	@Test
	public void testTLSCipherSuites() throws Exception {
		Assume.assumeTrue(PropsValues.FIPS_ENABLED);

		Set<String> tlsCipherSuites = new TreeSet<>();

		for (JSONObject protocolPropertiesJSONObject :
				_getProtocolPropertiesJSONObjects()) {

			JSONArray cipherSuitesJSONArray =
				protocolPropertiesJSONObject.getJSONArray("cipherSuites");

			for (int i = 0; i < cipherSuitesJSONArray.length(); i++) {
				JSONObject cipherSuiteJSONObject =
					cipherSuitesJSONArray.getJSONObject(i);

				tlsCipherSuites.add(cipherSuiteJSONObject.getString("name"));
			}
		}

		SSLParameters sslParameters = _getSupportedSSLParameters();

		Assert.assertEquals(
			SetUtil.fromArray(
				FIPSModeValidator.getAllowedTLSCipherSuites(
					sslParameters.getCipherSuites())),
			tlsCipherSuites);
	}

	@Test
	public void testTLSProtocols() throws Exception {
		Assume.assumeTrue(PropsValues.FIPS_ENABLED);

		Set<String> tlsProtocols = new TreeSet<>();

		for (JSONObject protocolPropertiesJSONObject :
				_getProtocolPropertiesJSONObjects()) {

			tlsProtocols.add(
				"TLSv" + protocolPropertiesJSONObject.getString("version"));
		}

		SSLParameters sslParameters = _getSupportedSSLParameters();

		Assert.assertEquals(
			SetUtil.fromArray(
				FIPSModeValidator.getAllowedTLSProtocols(
					sslParameters.getProtocols())),
			tlsProtocols);
	}

	private String _fetchFIPSProviderName(JSONObject jsonObject) {
		JSONObject metadataJSONObject = jsonObject.getJSONObject("metadata");

		JSONArray propertiesJSONArray = metadataJSONObject.getJSONArray(
			"properties");

		for (int i = 0; i < propertiesJSONArray.length(); i++) {
			JSONObject propertyJSONObject = propertiesJSONArray.getJSONObject(
				i);

			if (Objects.equals(
					propertyJSONObject.getString("name"),
					"liferay:cbom:fips-provider")) {

				return StringUtil.extractFirst(
					propertyJSONObject.getString("value"), CharPool.SPACE);
			}
		}

		return null;
	}

	private JSONObject _getComponentJSONObject(String bomRef) {
		JSONArray componentsJSONArray = _cbomJSONObject.getJSONArray(
			"components");

		for (int i = 0; i < componentsJSONArray.length(); i++) {
			JSONObject componentJSONObject = componentsJSONArray.getJSONObject(
				i);

			if (Objects.equals(
					componentJSONObject.getString("bom-ref"), bomRef)) {

				return componentJSONObject;
			}
		}

		return null;
	}

	private List<String> _getDependsOn(String ref) {
		JSONArray dependenciesJSONArray = _cbomJSONObject.getJSONArray(
			"dependencies");

		for (int i = 0; i < dependenciesJSONArray.length(); i++) {
			JSONObject dependencyJSONObject =
				dependenciesJSONArray.getJSONObject(i);

			if (Objects.equals(dependencyJSONObject.getString("ref"), ref)) {
				return JSONUtil.toStringList(
					dependencyJSONObject.getJSONArray("dependsOn"));
			}
		}

		return Collections.emptyList();
	}

	private List<JSONObject> _getProtocolPropertiesJSONObjects() {
		List<JSONObject> protocolPropertiesJSONObjects = new ArrayList<>();

		JSONArray componentsJSONArray = _cbomJSONObject.getJSONArray(
			"components");

		for (int i = 0; i < componentsJSONArray.length(); i++) {
			JSONObject componentJSONObject = componentsJSONArray.getJSONObject(
				i);

			JSONObject cryptoPropertiesJSONObject =
				componentJSONObject.getJSONObject("cryptoProperties");

			if ((cryptoPropertiesJSONObject == null) ||
				!Objects.equals(
					cryptoPropertiesJSONObject.getString("assetType"),
					"protocol")) {

				continue;
			}

			JSONObject protocolPropertiesJSONObject =
				cryptoPropertiesJSONObject.getJSONObject("protocolProperties");

			if (!protocolPropertiesJSONObject.has("version")) {
				continue;
			}

			protocolPropertiesJSONObjects.add(protocolPropertiesJSONObject);
		}

		return protocolPropertiesJSONObjects;
	}

	private Provider _getProvider(String algorithm, String jcaClassName) {
		if (Objects.equals(jcaClassName, "SecureRandom")) {
			SecureRandom secureRandom = new SecureRandom();

			return secureRandom.getProvider();
		}

		Object object = ReflectionTestUtil.invoke(
			_jcaClasses.get(jcaClassName), "getInstance",
			new Class<?>[] {String.class}, algorithm);

		return ReflectionTestUtil.invoke(
			object, "getProvider", new Class<?>[0]);
	}

	private SSLParameters _getSupportedSSLParameters() throws Exception {
		SSLContext sslContext = SSLContext.getDefault();

		return sslContext.getSupportedSSLParameters();
	}

	private String _getVersion(Provider provider) {
		Class<?> clazz = provider.getClass();

		Package pkg = clazz.getPackage();

		if (pkg.getImplementationVersion() != null) {
			return pkg.getImplementationVersion();
		}

		return provider.getVersionStr();
	}

	private static final File _cbomDir = new File(
		PropsValues.LIFERAY_HOME, "cbom");
	private static final Map<String, Class<?>> _jcaClasses =
		HashMapBuilder.<String, Class<?>>put(
			"Cipher", Cipher.class
		).put(
			"KeyAgreement", KeyAgreement.class
		).put(
			"KeyFactory", KeyFactory.class
		).put(
			"KeyGenerator", KeyGenerator.class
		).put(
			"KeyManagerFactory", KeyManagerFactory.class
		).put(
			"KeyPairGenerator", KeyPairGenerator.class
		).put(
			"KeyStore", KeyStore.class
		).put(
			"Mac", Mac.class
		).put(
			"MessageDigest", MessageDigest.class
		).put(
			"SecretKeyFactory", SecretKeyFactory.class
		).put(
			"Signature", Signature.class
		).put(
			"SSLContext", SSLContext.class
		).put(
			"TrustManagerFactory", TrustManagerFactory.class
		).build();

	private JSONObject _cbomJSONObject;

}