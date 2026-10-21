/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.UnsafeFunction;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Signature;

import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Asks the JVM which JCA provider answers an algorithm, the same way the
 * portal's own calls to <code>getInstance</code> do.
 *
 * @author Manuele Castro
 */
public class CryptoProviderUtil {

	public static String fetchProviderName(
			String algorithm, String jcaClassName)
		throws Exception {

		if (!isSupportedJCAClassName(jcaClassName)) {
			return null;
		}

		try {
			return getProviderName(algorithm, jcaClassName);
		}
		catch (GeneralSecurityException generalSecurityException) {
			if (_log.isDebugEnabled()) {
				_log.debug(generalSecurityException);
			}

			return null;
		}
	}

	public static String getProviderName(String algorithm, String jcaClassName)
		throws Exception {

		UnsafeFunction<String, Provider, Exception> unsafeFunction =
			_providerUnsafeFunctions.get(jcaClassName);

		if (unsafeFunction == null) {
			throw new IllegalArgumentException(
				"Unsupported JCA class " + jcaClassName);
		}

		Provider provider = unsafeFunction.apply(algorithm);

		return provider.getName();
	}

	public static boolean isSupportedJCAClassName(String jcaClassName) {
		return _providerUnsafeFunctions.containsKey(jcaClassName);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CryptoProviderUtil.class);

	private static final Map
		<String, UnsafeFunction<String, Provider, Exception>>
			_providerUnsafeFunctions =
				HashMapBuilder.
					<String, UnsafeFunction<String, Provider, Exception>>put(
						"Cipher",
						algorithm -> {
							Cipher cipher = Cipher.getInstance(algorithm);

							return cipher.getProvider();
						}
					).put(
						"KeyAgreement",
						algorithm -> {
							KeyAgreement keyAgreement =
								KeyAgreement.getInstance(algorithm);

							return keyAgreement.getProvider();
						}
					).put(
						"KeyFactory",
						algorithm -> {
							KeyFactory keyFactory = KeyFactory.getInstance(
								algorithm);

							return keyFactory.getProvider();
						}
					).put(
						"KeyGenerator",
						algorithm -> {
							KeyGenerator keyGenerator =
								KeyGenerator.getInstance(algorithm);

							return keyGenerator.getProvider();
						}
					).put(
						"KeyManagerFactory",
						algorithm -> {
							KeyManagerFactory keyManagerFactory =
								KeyManagerFactory.getInstance(algorithm);

							return keyManagerFactory.getProvider();
						}
					).put(
						"KeyPairGenerator",
						algorithm -> {
							KeyPairGenerator keyPairGenerator =
								KeyPairGenerator.getInstance(algorithm);

							return keyPairGenerator.getProvider();
						}
					).put(
						"KeyStore",
						algorithm -> {
							KeyStore keyStore = KeyStore.getInstance(algorithm);

							return keyStore.getProvider();
						}
					).put(
						"Mac",
						algorithm -> {
							Mac mac = Mac.getInstance(algorithm);

							return mac.getProvider();
						}
					).put(
						"MessageDigest",
						algorithm -> {
							MessageDigest messageDigest =
								MessageDigest.getInstance(algorithm);

							return messageDigest.getProvider();
						}
					).put(
						"SecretKeyFactory",
						algorithm -> {
							SecretKeyFactory secretKeyFactory =
								SecretKeyFactory.getInstance(algorithm);

							return secretKeyFactory.getProvider();
						}
					).put(
						"Signature",
						algorithm -> {
							Signature signature = Signature.getInstance(
								algorithm);

							return signature.getProvider();
						}
					).put(
						"SSLContext",
						algorithm -> {
							SSLContext sslContext = SSLContext.getInstance(
								algorithm);

							return sslContext.getProvider();
						}
					).put(
						"TrustManagerFactory",
						algorithm -> {
							TrustManagerFactory trustManagerFactory =
								TrustManagerFactory.getInstance(algorithm);

							return trustManagerFactory.getProvider();
						}
					).build();

}