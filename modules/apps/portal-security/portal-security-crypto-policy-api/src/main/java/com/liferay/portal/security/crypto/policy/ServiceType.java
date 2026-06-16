/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy;

/**
 * Maps 1:1 to Java Security service type strings.
 *
 * @author Manuele Castro
 */
public enum ServiceType {

	CIPHER("Cipher"), KEY_FACTORY("KeyFactory"),
	KEY_GENERATOR("KeyGenerator"), KEY_PAIR_GENERATOR("KeyPairGenerator"),
	MAC("Mac"), MESSAGE_DIGEST("MessageDigest"),
	SECRET_KEY_FACTORY("SecretKeyFactory"), SIGNATURE("Signature");

	public String getServiceTypeName() {
		return _serviceTypeName;
	}

	ServiceType(String serviceTypeName) {
		_serviceTypeName = serviceTypeName;
	}

	private final String _serviceTypeName;

}
