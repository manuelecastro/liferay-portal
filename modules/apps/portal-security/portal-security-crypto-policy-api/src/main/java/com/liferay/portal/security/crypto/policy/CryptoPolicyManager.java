/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy;

import com.liferay.portal.security.crypto.policy.exception.CryptoPolicyException;

import java.util.Set;

/**
 * Provides runtime cryptographic policy information based on the JVM's
 * installed security providers. When FIPS mode is enabled, the check methods
 * enforce that only approved algorithms and key sizes are used.
 *
 * @author Manuele Castro
 */
public interface CryptoPolicyManager {

	/**
	 * Returns all algorithms the current runtime permits for the given service
	 * type. In a FIPS JVM the installed providers expose only FIPS-approved
	 * algorithms, so this set is naturally FIPS-constrained.
	 */
	public Set<String> getAllowedAlgorithms(ServiceType serviceType);

	/**
	 * Returns all key sizes the current runtime permits for the given
	 * algorithm, discovered empirically at activation time. Returns an empty
	 * set for algorithms that do not use a configurable key size (e.g.
	 * MessageDigest).
	 */
	public Set<Integer> getAllowedKeySizes(String algorithm);

	/**
	 * In non-FIPS mode: returns the algorithm unchanged.
	 * In FIPS mode: returns the algorithm if it is approved for the given
	 * service type, throws CryptoPolicyException otherwise.
	 */
	public String checkAlgorithm(String algorithm, ServiceType serviceType)
		throws CryptoPolicyException;

	/**
	 * In non-FIPS mode: returns the algorithm unchanged.
	 * In FIPS mode: returns the algorithm if both the algorithm and key size
	 * are approved, throws CryptoPolicyException otherwise.
	 */
	public String checkAlgorithm(
			String algorithm, int keySize, ServiceType serviceType)
		throws CryptoPolicyException;

	public boolean isFIPSMode();

}
