# CryptoPolicyManager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a runtime OSGi service that exposes approved cryptographic algorithms and key sizes, enforcing FIPS compliance when `fips.enabled=true`.

**Architecture:** Two new modules (`portal-security-crypto-policy-api` and `-impl`) under `modules/apps/portal-security/`. The impl enumerates algorithms from installed JCA providers and probes key sizes empirically at activation. Eight existing call sites are wired to call `checkAlgorithm()` before every `getInstance()` invocation.

**Tech Stack:** Java 17, OSGi DS (`@Component`, `@Reference`, `@Activate`), `java.security.*`, `javax.crypto.*`, JUnit 5.

---

## File Map

**New files:**
- `modules/apps/portal-security/portal-security-crypto-policy-api/.lfrbuild-portal`
- `modules/apps/portal-security/portal-security-crypto-policy-api/bnd.bnd`
- `modules/apps/portal-security/portal-security-crypto-policy-api/build.gradle`
- `modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/ServiceType.java`
- `modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/exception/CryptoPolicyException.java`
- `modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/CryptoPolicyManager.java`
- `modules/apps/portal-security/portal-security-crypto-policy-impl/.lfrbuild-portal`
- `modules/apps/portal-security/portal-security-crypto-policy-impl/bnd.bnd`
- `modules/apps/portal-security/portal-security-crypto-policy-impl/build.gradle`
- `modules/apps/portal-security/portal-security-crypto-policy-impl/src/main/java/com/liferay/portal/security/crypto/policy/internal/CryptoPolicyManagerImpl.java`
- `modules/apps/portal-security/portal-security-crypto-policy-impl/src/test/java/com/liferay/portal/security/crypto/policy/internal/CryptoPolicyManagerImplTest.java`

**Modified files:**
- `modules/apps/static/portal/portal-encryptor/build.gradle`
- `modules/apps/static/portal/portal-encryptor/src/main/java/com/liferay/portal/encryptor/EncryptorImpl.java`
- `modules/apps/portal-security/portal-security-password-encryptor-impl/build.gradle`
- `modules/apps/portal-security/portal-security-password-encryptor-impl/src/main/java/com/liferay/portal/security/password/encryptor/internal/SSHAPasswordEncryptor.java`
- `modules/apps/portal-crypto-hash/portal-crypto-hash-provider/portal-crypto-hash-provider-message-digest/build.gradle`
- `modules/apps/portal-crypto-hash/portal-crypto-hash-provider/portal-crypto-hash-provider-message-digest/src/main/java/com/liferay/portal/crypto/hash/provider/message/digest/internal/MessageDigestCryptoHashProviderFactory.java`
- `modules/apps/analytics/analytics-settings-impl/build.gradle`
- `modules/apps/analytics/analytics-settings-impl/src/main/java/com/liferay/analytics/settings/internal/security/auth/verifier/AnalyticsSecurityAuthVerifier.java`
- `modules/dxp/apps/multi-factor-authentication/multi-factor-authentication-timebased-otp-web/build.gradle`
- `modules/dxp/apps/multi-factor-authentication/multi-factor-authentication-timebased-otp-web/src/main/java/com/liferay/multi/factor/authentication/timebased/otp/web/internal/checker/TimeBasedOTPBrowserSetupMFAChecker.java`
- `modules/apps/digital-signature/digital-signature-impl/build.gradle`
- `modules/apps/digital-signature/digital-signature-impl/src/main/java/com/liferay/digital/signature/internal/http/DSHttp.java`
- `modules/dxp/apps/saml/saml-opensaml-integration/build.gradle`
- `modules/dxp/apps/saml/saml-opensaml-integration/src/main/java/com/liferay/saml/opensaml/integration/internal/certificate/CertificateToolImpl.java`

---

## Task 1: Scaffold `portal-security-crypto-policy-api`

**Files:**
- Create: `modules/apps/portal-security/portal-security-crypto-policy-api/.lfrbuild-portal`
- Create: `modules/apps/portal-security/portal-security-crypto-policy-api/bnd.bnd`
- Create: `modules/apps/portal-security/portal-security-crypto-policy-api/build.gradle`

- [ ] **Step 1: Create the module directory and marker file**

```bash
mkdir -p modules/apps/portal-security/portal-security-crypto-policy-api
touch modules/apps/portal-security/portal-security-crypto-policy-api/.lfrbuild-portal
```

- [ ] **Step 2: Create `bnd.bnd`**

```
Bundle-Name: Liferay Portal Security Crypto Policy API
Bundle-SymbolicName: com.liferay.portal.security.crypto.policy.api
Bundle-Version: 1.0.0
Export-Package:\
	com.liferay.portal.security.crypto.policy,\
	com.liferay.portal.security.crypto.policy.exception
```

- [ ] **Step 3: Create `build.gradle`**

```gradle
dependencies {
	compileOnly group: "com.liferay.portal", name: "com.liferay.portal.kernel", version: "default"
	compileOnly group: "org.osgi", name: "org.osgi.annotation.versioning", version: "1.1.0"
}
```

- [ ] **Step 4: Commit**

```bash
git add modules/apps/portal-security/portal-security-crypto-policy-api
git commit -m "LPD-82902 Scaffold portal-security-crypto-policy-api module"
```

---

## Task 2: Implement `ServiceType`

**Files:**
- Create: `modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/ServiceType.java`

- [ ] **Step 1: Create the source directory**

```bash
mkdir -p modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy
```

- [ ] **Step 2: Create `ServiceType.java`**

```java
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
	MAC("Mac"), MESSAGE_DIGEST("MessageDigest"), SECRET_KEY_FACTORY("SecretKeyFactory"),
	SIGNATURE("Signature");

	public String getServiceTypeName() {
		return _serviceTypeName;
	}

	ServiceType(String serviceTypeName) {
		_serviceTypeName = serviceTypeName;
	}

	private final String _serviceTypeName;

}
```

- [ ] **Step 3: Commit**

```bash
git add modules/apps/portal-security/portal-security-crypto-policy-api/src
git commit -m "LPD-82902 Add ServiceType enum"
```

---

## Task 3: Implement `CryptoPolicyException`

**Files:**
- Create: `modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/exception/CryptoPolicyException.java`

- [ ] **Step 1: Create the exception directory**

```bash
mkdir -p modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/exception
```

- [ ] **Step 2: Create `CryptoPolicyException.java`**

```java
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.exception;

/**
 * Thrown when a non-approved algorithm or key size is used in FIPS mode.
 *
 * @author Manuele Castro
 */
public class CryptoPolicyException extends RuntimeException {

	public CryptoPolicyException(String message) {
		super(message);
	}

}
```

- [ ] **Step 3: Commit**

```bash
git add modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/exception
git commit -m "LPD-82902 Add CryptoPolicyException"
```

---

## Task 4: Implement `CryptoPolicyManager` interface

**Files:**
- Create: `modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/CryptoPolicyManager.java`

- [ ] **Step 1: Create `CryptoPolicyManager.java`**

```java
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
```

- [ ] **Step 2: Verify compilation**

```bash
cd modules/apps/portal-security/portal-security-crypto-policy-api && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add modules/apps/portal-security/portal-security-crypto-policy-api/src/main/java/com/liferay/portal/security/crypto/policy/CryptoPolicyManager.java
git commit -m "LPD-82902 Add CryptoPolicyManager interface"
```

---

## Task 5: Scaffold `portal-security-crypto-policy-impl`

**Files:**
- Create: `modules/apps/portal-security/portal-security-crypto-policy-impl/.lfrbuild-portal`
- Create: `modules/apps/portal-security/portal-security-crypto-policy-impl/bnd.bnd`
- Create: `modules/apps/portal-security/portal-security-crypto-policy-impl/build.gradle`

- [ ] **Step 1: Create the module directory and marker file**

```bash
mkdir -p modules/apps/portal-security/portal-security-crypto-policy-impl
touch modules/apps/portal-security/portal-security-crypto-policy-impl/.lfrbuild-portal
```

- [ ] **Step 2: Create `bnd.bnd`**

```
Bundle-Name: Liferay Portal Security Crypto Policy Implementation
Bundle-SymbolicName: com.liferay.portal.security.crypto.policy.impl
Bundle-Version: 1.0.0
```

- [ ] **Step 3: Create `build.gradle`**

```gradle
dependencies {
	compileOnly group: "com.liferay.portal", name: "com.liferay.portal.kernel", version: "default"
	compileOnly group: "org.osgi", name: "org.osgi.service.component.annotations", version: "1.4.0"
	compileOnly group: "org.osgi", name: "osgi.core", version: "6.0.0"
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
}
```

- [ ] **Step 4: Commit**

```bash
git add modules/apps/portal-security/portal-security-crypto-policy-impl
git commit -m "LPD-82902 Scaffold portal-security-crypto-policy-impl module"
```

---

## Task 6: Write failing unit tests for `CryptoPolicyManagerImpl`

**Files:**
- Create: `modules/apps/portal-security/portal-security-crypto-policy-impl/src/test/java/com/liferay/portal/security/crypto/policy/internal/CryptoPolicyManagerImplTest.java`

- [ ] **Step 1: Create the test directory**

```bash
mkdir -p modules/apps/portal-security/portal-security-crypto-policy-impl/src/test/java/com/liferay/portal/security/crypto/policy/internal
```

- [ ] **Step 2: Create `CryptoPolicyManagerImplTest.java`**

```java
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.internal;

import com.liferay.portal.security.crypto.policy.ServiceType;
import com.liferay.portal.security.crypto.policy.exception.CryptoPolicyException;

import java.security.Provider;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoPolicyManagerImplTest {

	@Before
	public void setUp() {
		_cryptoPolicyManagerImpl = new CryptoPolicyManagerImpl();

		_cryptoPolicyManagerImpl._buildAlgorithmMap(
			new Provider[] {new _MockFIPSProvider()});
		_cryptoPolicyManagerImpl._probeKeySizes(
			new Provider[] {new _MockFIPSProvider()});
	}

	@Test
	public void testGetAllowedAlgorithmsReturnsOnlyExposedAlgorithms() {
		Set<String> algorithms =
			_cryptoPolicyManagerImpl.getAllowedAlgorithms(
				ServiceType.MESSAGE_DIGEST);

		Assert.assertTrue(algorithms.contains("SHA-256"));
		Assert.assertFalse(algorithms.contains("MD5"));
		Assert.assertFalse(algorithms.contains("SHA-1"));
	}

	@Test
	public void testGetAllowedAlgorithmsReturnsEmptySetForUnknownServiceType() {
		Set<String> algorithms =
			_cryptoPolicyManagerImpl.getAllowedAlgorithms(ServiceType.MAC);

		Assert.assertTrue(algorithms.isEmpty());
	}

	@Test
	public void testCheckAlgorithmNonFIPSAlwaysReturnsAlgorithm() {

		// Non-FIPS impl always returns the algorithm unchanged

		String result = _cryptoPolicyManagerImpl.checkAlgorithm(
			"MD5", ServiceType.MESSAGE_DIGEST);

		Assert.assertEquals("MD5", result);
	}

	@Test
	public void testCheckAlgorithmFIPSApprovedReturnsAlgorithm() {
		CryptoPolicyManagerImpl fipsImpl = new _FIPSEnabledImpl();

		fipsImpl._buildAlgorithmMap(new Provider[] {new _MockFIPSProvider()});
		fipsImpl._probeKeySizes(new Provider[] {new _MockFIPSProvider()});

		String result = fipsImpl.checkAlgorithm(
			"SHA-256", ServiceType.MESSAGE_DIGEST);

		Assert.assertEquals("SHA-256", result);
	}

	@Test(expected = CryptoPolicyException.class)
	public void testCheckAlgorithmFIPSNonApprovedThrows() {
		CryptoPolicyManagerImpl fipsImpl = new _FIPSEnabledImpl();

		fipsImpl._buildAlgorithmMap(new Provider[] {new _MockFIPSProvider()});
		fipsImpl._probeKeySizes(new Provider[] {new _MockFIPSProvider()});

		fipsImpl.checkAlgorithm("MD5", ServiceType.MESSAGE_DIGEST);
	}

	@Test
	public void testCheckAlgorithmWithKeySizeNonFIPSAlwaysReturnsAlgorithm() {

		// Non-FIPS mode: key size check is skipped regardless of allowed sizes

		String result = _cryptoPolicyManagerImpl.checkAlgorithm(
			"AES", 192, ServiceType.KEY_GENERATOR);

		Assert.assertEquals("AES", result);
	}

	private CryptoPolicyManagerImpl _cryptoPolicyManagerImpl;

	private static class _FIPSEnabledImpl extends CryptoPolicyManagerImpl {

		@Override
		protected boolean _isFIPSEnabled() {
			return true;
		}

	}

	private static class _MockFIPSProvider extends Provider {

		_MockFIPSProvider() {
			super("MockFIPS", "1.0", "Mock FIPS provider for testing");

			putService(
				new Service(
					this, "MessageDigest", "SHA-256",
					_MockImpl.class.getName(), null, null));
			putService(
				new Service(
					this, "Cipher", "AES", _MockImpl.class.getName(), null,
					null));
			putService(
				new Service(
					this, "KeyGenerator", "AES", _MockImpl.class.getName(),
					null, null));
		}

		public static class _MockImpl {
		}

	}

}
```

- [ ] **Step 3: Run the tests — expect failure (class does not exist yet)**

```bash
cd modules/apps/portal-security/portal-security-crypto-policy-impl && gw test --tests "com.liferay.portal.security.crypto.policy.internal.CryptoPolicyManagerImplTest"
```

Expected: `FAILED` — `CryptoPolicyManagerImpl` does not exist yet.

- [ ] **Step 4: Commit**

```bash
git add modules/apps/portal-security/portal-security-crypto-policy-impl/src/test
git commit -m "LPD-82902 Add failing unit tests for CryptoPolicyManagerImpl"
```

---

## Task 7: Implement `CryptoPolicyManagerImpl`

**Files:**
- Create: `modules/apps/portal-security/portal-security-crypto-policy-impl/src/main/java/com/liferay/portal/security/crypto/policy/internal/CryptoPolicyManagerImpl.java`

- [ ] **Step 1: Create the source directory**

```bash
mkdir -p modules/apps/portal-security/portal-security-crypto-policy-impl/src/main/java/com/liferay/portal/security/crypto/policy/internal
```

- [ ] **Step 2: Create `CryptoPolicyManagerImpl.java`**

```java
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.internal;

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
	public String checkAlgorithm(String algorithm, ServiceType serviceType) {
		if (!_isFIPSEnabled()) {
			return algorithm;
		}

		if (!getAllowedAlgorithms(serviceType).contains(algorithm)) {
			throw new CryptoPolicyException(
				"Algorithm \"" + algorithm + "\" is not approved in FIPS mode");
		}

		return algorithm;
	}

	@Override
	public String checkAlgorithm(
		String algorithm, int keySize, ServiceType serviceType) {

		checkAlgorithm(algorithm, serviceType);

		if (_isFIPSEnabled() &&
			!getAllowedKeySizes(algorithm).contains(keySize)) {

			throw new CryptoPolicyException(
				"Key size " + keySize + " for algorithm \"" + algorithm +
					"\" is not approved in FIPS mode");
		}

		return algorithm;
	}

	@Override
	public boolean isFIPSMode() {
		return _isFIPSEnabled();
	}

	@Activate
	private void _activate() {
		_buildAlgorithmMap(Security.getProviders());
		_probeKeySizes(Security.getProviders());
	}

	void _buildAlgorithmMap(Provider[] providers) {
		Map<ServiceType, Set<String>> algorithmMap = new EnumMap<>(
			ServiceType.class);

		for (Provider provider : providers) {
			for (Provider.Service service : provider.getServices()) {
				ServiceType serviceType = _SERVICE_TYPE_MAP.get(
					service.getType());

				if (serviceType != null) {
					algorithmMap.computeIfAbsent(
						serviceType, k -> new LinkedHashSet<>()
					).add(
						service.getAlgorithm()
					);
				}
			}
		}

		_algorithmMap = algorithmMap;
	}

	protected boolean _isFIPSEnabled() {
		return PropsValues.FIPS_ENABLED;
	}

	void _probeKeySizes(Provider[] providers) {
		Map<String, Set<Integer>> keySizesMap = new HashMap<>();

		for (Provider provider : providers) {
			for (Provider.Service service : provider.getServices()) {
				String type = service.getType();
				String algorithm = service.getAlgorithm();

				if ("KeyGenerator".equals(type)) {
					Set<Integer> validSizes = new TreeSet<>();

					for (int size : _SYMMETRIC_PROBE_SIZES) {
						try {
							KeyGenerator keyGenerator =
								KeyGenerator.getInstance(algorithm);

							keyGenerator.init(size);

							validSizes.add(size);
						}
						catch (InvalidParameterException |
							   NoSuchAlgorithmException ignored) {
						}
					}

					if (!validSizes.isEmpty()) {
						keySizesMap.put(algorithm, validSizes);
					}
				}
				else if ("KeyPairGenerator".equals(type)) {
					Set<Integer> validSizes = new TreeSet<>();

					for (int size : _ASYMMETRIC_PROBE_SIZES) {
						try {
							KeyPairGenerator keyPairGenerator =
								KeyPairGenerator.getInstance(algorithm);

							keyPairGenerator.initialize(size);

							validSizes.add(size);
						}
						catch (InvalidParameterException |
							   NoSuchAlgorithmException ignored) {
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

	private static final int[] _ASYMMETRIC_PROBE_SIZES = {
		512, 1024, 2048, 3072, 4096
	};

	private static final int[] _SYMMETRIC_PROBE_SIZES = {
		40, 56, 64, 112, 128, 168, 192, 256, 512
	};

	private static final Map<String, ServiceType> _SERVICE_TYPE_MAP;

	static {
		Map<String, ServiceType> map = new HashMap<>();

		for (ServiceType serviceType : ServiceType.values()) {
			map.put(serviceType.getServiceTypeName(), serviceType);
		}

		_SERVICE_TYPE_MAP = Collections.unmodifiableMap(map);
	}

	private Map<ServiceType, Set<String>> _algorithmMap =
		Collections.emptyMap();
	private Map<String, Set<Integer>> _allowedKeySizesMap =
		Collections.emptyMap();

}
```

- [ ] **Step 3: Run the tests — expect pass**

```bash
cd modules/apps/portal-security/portal-security-crypto-policy-impl && gw test --tests "com.liferay.portal.security.crypto.policy.internal.CryptoPolicyManagerImplTest"
```

Expected: `BUILD SUCCESSFUL` — all tests pass.

- [ ] **Step 4: Commit**

```bash
git add modules/apps/portal-security/portal-security-crypto-policy-impl/src/main
git commit -m "LPD-82902 Implement CryptoPolicyManagerImpl"
```

---

## Task 8: Wire `EncryptorImpl`

**Files:**
- Modify: `modules/apps/static/portal/portal-encryptor/build.gradle`
- Modify: `modules/apps/static/portal/portal-encryptor/src/main/java/com/liferay/portal/encryptor/EncryptorImpl.java`

- [ ] **Step 1: Add the API dependency to `build.gradle`**

Add to the existing `dependencies` block in `modules/apps/static/portal/portal-encryptor/build.gradle`:

```gradle
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
```

- [ ] **Step 2: Add `@Activate`, `@Reference`, and `checkAlgorithm` to `EncryptorImpl`**

Add these imports to `EncryptorImpl.java`:

```java
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
```

Add this `@Activate` method before the existing `decrypt` method:

```java
@Activate
private void _activate() {
	_cryptoPolicyManager.checkAlgorithm(
		KEY_ALGORITHM, KEY_SIZE, ServiceType.KEY_GENERATOR);
	_cryptoPolicyManager.checkAlgorithm(KEY_ALGORITHM, ServiceType.CIPHER);
}
```

Add this field at the bottom of the class, before the existing private fields:

```java
@Reference
private CryptoPolicyManager _cryptoPolicyManager;
```

- [ ] **Step 3: Compile to verify**

```bash
cd modules/apps/static/portal/portal-encryptor && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add modules/apps/static/portal/portal-encryptor
git commit -m "LPD-82902 Wire CryptoPolicyManager into EncryptorImpl"
```

---

## Task 9: Wire `SSHAPasswordEncryptor`

**Files:**
- Modify: `modules/apps/portal-security/portal-security-password-encryptor-impl/build.gradle`
- Modify: `modules/apps/portal-security/portal-security-password-encryptor-impl/src/main/java/com/liferay/portal/security/password/encryptor/internal/SSHAPasswordEncryptor.java`

- [ ] **Step 1: Add the API dependency to `build.gradle`**

Add to the existing `dependencies` block in `modules/apps/portal-security/portal-security-password-encryptor-impl/build.gradle`:

```gradle
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
```

- [ ] **Step 2: Add imports to `SSHAPasswordEncryptor.java`**

```java
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
```

- [ ] **Step 3: Add `@Activate` method to `SSHAPasswordEncryptor.java`**

Add before the existing `encrypt` method:

```java
@Activate
private void _activate() {
	_cryptoPolicyManager.checkAlgorithm("SHA-1", ServiceType.MESSAGE_DIGEST);
}
```

- [ ] **Step 4: Add `@Reference` field to `SSHAPasswordEncryptor.java`**

Add at the bottom of the class:

```java
@Reference
private CryptoPolicyManager _cryptoPolicyManager;
```

- [ ] **Step 5: Compile to verify**

```bash
cd modules/apps/portal-security/portal-security-password-encryptor-impl && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/apps/portal-security/portal-security-password-encryptor-impl
git commit -m "LPD-82902 Wire CryptoPolicyManager into SSHAPasswordEncryptor"
```

---

## Task 10: Wire `MessageDigestCryptoHashProviderFactory`

**Files:**
- Modify: `modules/apps/portal-crypto-hash/portal-crypto-hash-provider/portal-crypto-hash-provider-message-digest/build.gradle`
- Modify: `modules/apps/portal-crypto-hash/portal-crypto-hash-provider/portal-crypto-hash-provider-message-digest/src/main/java/com/liferay/portal/crypto/hash/provider/message/digest/internal/MessageDigestCryptoHashProviderFactory.java`

- [ ] **Step 1: Add the API dependency to `build.gradle`**

Add to the existing `dependencies` block:

```gradle
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
```

- [ ] **Step 2: Add imports to `MessageDigestCryptoHashProviderFactory.java`**

```java
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;

import org.osgi.service.component.annotations.Reference;
```

- [ ] **Step 3: Wrap the algorithm in the `create` method**

In `MessageDigestCryptoHashProviderFactory.create()`, both the null and non-null branches must check the algorithm before constructing the inner provider. Replace the entire `try` block body:

```java
if (cryptoHashProviderProperties == null) {
    _cryptoPolicyManager.checkAlgorithm(
        "SHA-256", ServiceType.MESSAGE_DIGEST);

    return new MessageDigestCryptoHashProvider(Collections.emptyMap());
}

_cryptoPolicyManager.checkAlgorithm(
    MapUtil.getString(
        cryptoHashProviderProperties, "message.digest.algorithm",
        "SHA-256"),
    ServiceType.MESSAGE_DIGEST);

return new MessageDigestCryptoHashProvider(cryptoHashProviderProperties);
```

- [ ] **Step 4: Add `@Reference` field**

Add at the bottom of `MessageDigestCryptoHashProviderFactory`:

```java
@Reference
private CryptoPolicyManager _cryptoPolicyManager;
```

- [ ] **Step 5: Compile to verify**

```bash
cd modules/apps/portal-crypto-hash/portal-crypto-hash-provider/portal-crypto-hash-provider-message-digest && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/apps/portal-crypto-hash/portal-crypto-hash-provider/portal-crypto-hash-provider-message-digest
git commit -m "LPD-82902 Wire CryptoPolicyManager into MessageDigestCryptoHashProviderFactory"
```

---

## Task 11: Wire `AnalyticsSecurityAuthVerifier`

**Files:**
- Modify: `modules/apps/analytics/analytics-settings-impl/build.gradle`
- Modify: `modules/apps/analytics/analytics-settings-impl/src/main/java/com/liferay/analytics/settings/internal/security/auth/verifier/AnalyticsSecurityAuthVerifier.java`

- [ ] **Step 1: Add the API dependency to `build.gradle`**

Add to the existing `dependencies` block in `modules/apps/analytics/analytics-settings-impl/build.gradle`:

```gradle
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
```

- [ ] **Step 2: Add imports to `AnalyticsSecurityAuthVerifier.java`**

```java
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
```

- [ ] **Step 3: Add `@Activate` method**

Add before `_validateSignature`:

```java
@Activate
private void _activate() {
	_cryptoPolicyManager.checkAlgorithm("DSA", ServiceType.SIGNATURE);
	_cryptoPolicyManager.checkAlgorithm("DSA", ServiceType.KEY_FACTORY);
}
```

- [ ] **Step 4: Add `@Reference` field**

Add at the bottom of the class alongside the existing `@Reference` fields:

```java
@Reference
private CryptoPolicyManager _cryptoPolicyManager;
```

- [ ] **Step 5: Compile to verify**

```bash
cd modules/apps/analytics/analytics-settings-impl && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/apps/analytics/analytics-settings-impl
git commit -m "LPD-82902 Wire CryptoPolicyManager into AnalyticsSecurityAuthVerifier"
```

---

## Task 12: Wire `TimeBasedOTPBrowserSetupMFAChecker` (MFA)

**Files:**
- Modify: `modules/dxp/apps/multi-factor-authentication/multi-factor-authentication-timebased-otp-web/build.gradle`
- Modify: `modules/dxp/apps/multi-factor-authentication/multi-factor-authentication-timebased-otp-web/src/main/java/com/liferay/multi/factor/authentication/timebased/otp/web/internal/checker/TimeBasedOTPBrowserSetupMFAChecker.java`

`MFATimeBasedOTPUtil` is a static utility class, so the check is placed in its caller component.

- [ ] **Step 1: Add the API dependency to `build.gradle`**

Add to the existing `dependencies` block in `modules/dxp/apps/multi-factor-authentication/multi-factor-authentication-timebased-otp-web/build.gradle`:

```gradle
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
```

- [ ] **Step 2: Add imports to `TimeBasedOTPBrowserSetupMFAChecker.java`**

```java
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;

import com.liferay.multi.factor.authentication.timebased.otp.web.internal.util.MFATimeBasedOTPUtil;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
```

- [ ] **Step 3: Add `@Activate` method**

Add an `@Activate` method to `TimeBasedOTPBrowserSetupMFAChecker`:

```java
@Activate
private void _activate() {
	_cryptoPolicyManager.checkAlgorithm(
		MFATimeBasedOTPUtil.MFA_TIMEBASED_OTP_ALGORITHM, ServiceType.MAC);
}
```

- [ ] **Step 4: Add `@Reference` field**

Add alongside the existing `@Reference` fields:

```java
@Reference
private CryptoPolicyManager _cryptoPolicyManager;
```

- [ ] **Step 5: Compile to verify**

```bash
cd modules/dxp/apps/multi-factor-authentication/multi-factor-authentication-timebased-otp-web && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/dxp/apps/multi-factor-authentication/multi-factor-authentication-timebased-otp-web
git commit -m "LPD-82902 Wire CryptoPolicyManager into TimeBasedOTPBrowserSetupMFAChecker"
```

---

## Task 13: Wire `DSHttp` (Digital Signature)

**Files:**
- Modify: `modules/apps/digital-signature/digital-signature-impl/build.gradle`
- Modify: `modules/apps/digital-signature/digital-signature-impl/src/main/java/com/liferay/digital/signature/internal/http/DSHttp.java`

`DSAccessTokenWebCacheItem` is not an OSGi component — the check is placed in its owner `DSHttp`.

- [ ] **Step 1: Add the API dependency to `build.gradle`**

Add to the existing `dependencies` block in `modules/apps/digital-signature/digital-signature-impl/build.gradle`:

```gradle
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
```

- [ ] **Step 2: Add imports to `DSHttp.java`**

```java
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
```

- [ ] **Step 3: Add `@Activate` method to `DSHttp.java`**

```java
@Activate
private void _activate() {
	_cryptoPolicyManager.checkAlgorithm(
		"SHA256withRSA", ServiceType.SIGNATURE);
	_cryptoPolicyManager.checkAlgorithm("RSA", ServiceType.KEY_FACTORY);
}
```

- [ ] **Step 4: Add `@Reference` field**

Add alongside the existing `@Reference` fields:

```java
@Reference
private CryptoPolicyManager _cryptoPolicyManager;
```

- [ ] **Step 5: Compile to verify**

```bash
cd modules/apps/digital-signature/digital-signature-impl && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/apps/digital-signature/digital-signature-impl
git commit -m "LPD-82902 Wire CryptoPolicyManager into DSHttp"
```

---

## Task 14: Wire `CertificateToolImpl` (SAML)

**Files:**
- Modify: `modules/dxp/apps/saml/saml-opensaml-integration/build.gradle`
- Modify: `modules/dxp/apps/saml/saml-opensaml-integration/src/main/java/com/liferay/saml/opensaml/integration/internal/certificate/CertificateToolImpl.java`

- [ ] **Step 1: Add the API dependency to `build.gradle`**

Add to the existing `dependencies` block in `modules/dxp/apps/saml/saml-opensaml-integration/build.gradle`:

```gradle
	compileOnly project(":apps:portal-security:portal-security-crypto-policy-api")
```

- [ ] **Step 2: Add imports to `CertificateToolImpl.java`**

```java
import com.liferay.portal.security.crypto.policy.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.ServiceType;

import org.osgi.service.component.annotations.Reference;
```

- [ ] **Step 3: Wrap algorithm in `generateKeyPair`**

The existing `generateKeyPair` method in `CertificateToolImpl.java` starts with `KeyPairGenerator.getInstance(algorithm)`. Add the check before it:

```java
@Override
public KeyPair generateKeyPair(String algorithm, int keySize)
	throws CertificateToolException {

	_cryptoPolicyManager.checkAlgorithm(
		algorithm, keySize, ServiceType.KEY_PAIR_GENERATOR);

	// existing implementation continues unchanged below
```

- [ ] **Step 4: Add `@Reference` field**

Add at the bottom of the class:

```java
@Reference
private CryptoPolicyManager _cryptoPolicyManager;
```

- [ ] **Step 5: Compile to verify**

```bash
cd modules/dxp/apps/saml/saml-opensaml-integration && gw compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add modules/dxp/apps/saml/saml-opensaml-integration
git commit -m "LPD-82902 Wire CryptoPolicyManager into CertificateToolImpl"
```

---

## Task 15: Run format-source and final verification

- [ ] **Step 1: Run format-source on all changed modules**

```bash
/format-source
```

- [ ] **Step 2: Run unit tests for the impl module**

```bash
cd modules/apps/portal-security/portal-security-crypto-policy-impl && gw test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit any formatter changes**

```bash
git add -p
git commit -m "LPD-82902 Format source"
```
