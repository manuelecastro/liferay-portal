/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.internal;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;
import com.liferay.portal.security.crypto.policy.exception.CryptoPolicyException;

import java.security.InvalidParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.KeyGenerator;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * @author Manuele Castro
 */
@Component(service = CryptoPolicyManager.class)
public class CryptoPolicyManagerImpl implements CryptoPolicyManager {

	@Override
	public String checkAlgorithm(
		String algorithm, int keySize, ServiceType serviceType) {

		checkAlgorithm(algorithm, serviceType);

		if (isFIPSEnabled() &&
			!getAllowedKeySizes(
				algorithm
			).contains(
				keySize
			)) {

			throw new CryptoPolicyException(
				"Key size " + keySize + " for algorithm \"" + algorithm +
					"\" is not approved in FIPS mode");
		}

		return algorithm;
	}

	@Override
	public String checkAlgorithm(String algorithm, ServiceType serviceType) {
		if (!isFIPSEnabled()) {
			return algorithm;
		}

		if (!getAllowedAlgorithms(
				serviceType
		).contains(
			algorithm
		)) {

			throw new CryptoPolicyException(
				"Algorithm \"" + algorithm + "\" is not approved in FIPS mode");
		}

		return algorithm;
	}

	@Override
	public Set<String> getAllowedAlgorithms(ServiceType serviceType) {
		return Collections.unmodifiableSet(
			_algorithmMap.getOrDefault(serviceType, Collections.emptySet()));
	}

	@Override
	public Set<Integer> getAllowedKeySizes(String algorithm) {
		return Collections.unmodifiableSet(
			_allowedKeySizesMap.getOrDefault(
				algorithm, Collections.emptySet()));
	}

	@Override
	public boolean isFIPSMode() {
		return isFIPSEnabled();
	}

	@Activate
	protected void activate() {
		buildAlgorithmMap(Security.getProviders());
		buildKeySizeMap(Security.getProviders());
	}

	protected void buildAlgorithmMap(Provider[] providers) {
		Map<ServiceType, Set<String>> algorithmMap = new EnumMap<>(
			ServiceType.class);

		for (Provider provider : providers) {
			for (Provider.Service service : provider.getServices()) {
				ServiceType serviceType = _serviceTypeMap.get(
					service.getType());

				if (serviceType != null) {
					algorithmMap.computeIfAbsent(
						serviceType, serviceTypeName -> new LinkedHashSet<>()
					).add(
						service.getAlgorithm()
					);
				}
			}
		}

		_algorithmMap = algorithmMap;
	}

	protected void buildKeySizeMap(Provider[] providers) {
		Map<String, Set<Integer>> keySizesMap = new HashMap<>();

		for (Provider provider : providers) {
			for (Provider.Service service : provider.getServices()) {
				String algorithm = service.getAlgorithm();
				String type = service.getType();

				if (type.equals("KeyGenerator")) {
					Set<Integer> validSizes = new TreeSet<>();

					for (int size : _SYMMETRIC_PROBE_SIZES) {
						try {
							KeyGenerator keyGenerator =
								KeyGenerator.getInstance(algorithm);

							keyGenerator.init(size);

							validSizes.add(size);
						}
						catch (InvalidParameterException |
							   NoSuchAlgorithmException exception) {

							if (_log.isDebugEnabled()) {
								_log.debug(exception);
							}
						}
					}

					if (!validSizes.isEmpty()) {
						keySizesMap.put(algorithm, validSizes);
					}
				}
				else if (type.equals("KeyPairGenerator")) {
					Set<Integer> validSizes = new TreeSet<>();

					for (int size : _ASYMMETRIC_PROBE_SIZES) {
						try {
							KeyPairGenerator keyPairGenerator =
								KeyPairGenerator.getInstance(algorithm);

							keyPairGenerator.initialize(size);

							validSizes.add(size);
						}
						catch (InvalidParameterException |
							   NoSuchAlgorithmException exception) {

							if (_log.isDebugEnabled()) {
								_log.debug(exception);
							}
						}
					}

					if (!validSizes.isEmpty()) {
						keySizesMap.put(algorithm, validSizes);
					}
				}
			}
		}

		_allowedKeySizesMap = keySizesMap;
	}

	protected boolean isFIPSEnabled() {
		return PropsValues.FIPS_ENABLED;
	}

	private static final int[] _ASYMMETRIC_PROBE_SIZES = {
		512, 1024, 2048, 3072, 4096
	};

	private static final int[] _SYMMETRIC_PROBE_SIZES = {
		40, 56, 64, 112, 128, 168, 192, 256, 512
	};

	private static final Log _log = LogFactoryUtil.getLog(
		CryptoPolicyManagerImpl.class);

	private static final Map<String, ServiceType> _serviceTypeMap;

	static {
		Map<String, ServiceType> map = new HashMap<>();

		for (ServiceType serviceType : ServiceType.values()) {
			map.put(serviceType.getServiceTypeName(), serviceType);
		}

		_serviceTypeMap = Collections.unmodifiableMap(map);
	}

	private Map<ServiceType, Set<String>> _algorithmMap =
		Collections.emptyMap();
	private Map<String, Set<Integer>> _allowedKeySizesMap =
		Collections.emptyMap();

}