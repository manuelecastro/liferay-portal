# CryptoPolicyManager Design

## Context

Liferay Portal needs a runtime service that exposes which cryptographic algorithms
and key sizes are approved for use in the current environment. When a customer runs
the portal in a FIPS environment (`fips.enabled=true`), the service must enforce that
only FIPS-approved algorithms are used — failing loudly when a non-approved algorithm
is requested. In non-FIPS environments the service is a passive informational query.

Related ticket: LPD-82902

## Constraints

- Provider-agnostic: no imports from BCFIPS, Amazon Corretto, or any FIPS-specific
  provider packages. All code uses only `java.security.*` and `javax.crypto.*`.
- Dynamic: algorithm and key size discovery is derived at runtime from the installed
  providers, not from static configuration files.
- `PropsValues.FIPS_ENABLED` is the authoritative gate for FIPS mode. When it is
  `true`, the portal is assumed to be running on a FIPS-configured JVM.

## Module Structure

Two new modules under `modules/apps/portal-security/`:

```
portal-security-crypto-policy-api/
  .lfrbuild-portal
  bnd.bnd
  build.gradle
  src/main/java/com/liferay/portal/security/crypto/policy/
    CryptoPolicyManager.java
    ServiceType.java
    exception/
      CryptoPolicyException.java

portal-security-crypto-policy-impl/
  .lfrbuild-portal
  bnd.bnd
  build.gradle
  src/main/java/com/liferay/portal/security/crypto/policy/internal/
    CryptoPolicyManagerImpl.java
```

No SPI layer is needed — there is no pluggable behavior.

## API

### `ServiceType`

Maps 1:1 to Java Security service type strings. Used to scope algorithm queries to
a specific cryptographic operation.

```java
public enum ServiceType {
    CIPHER,
    KEY_GENERATOR,
    KEY_PAIR_GENERATOR,
    MESSAGE_DIGEST,
    MAC,
    SIGNATURE,
    SECRET_KEY_FACTORY
}
```

### `CryptoPolicyException`

Unchecked exception thrown when a non-approved algorithm or key size is requested
while the portal is running in FIPS mode.

```java
public class CryptoPolicyException extends RuntimeException {
    public CryptoPolicyException(String message) { super(message); }
}
```

### `CryptoPolicyManager`

```java
public interface CryptoPolicyManager {

    /**
     * Returns all algorithms the current runtime permits for the given service
     * type. In a FIPS JVM the installed providers expose only FIPS-approved
     * algorithms, so this set is naturally FIPS-constrained.
     */
    Set<String> getAllowedAlgorithms(ServiceType serviceType);

    /**
     * Returns all key sizes the current runtime permits for the given algorithm,
     * discovered empirically at activation time. Returns an empty set for
     * algorithms that do not use a configurable key size (e.g. MessageDigest).
     */
    Set<Integer> getAllowedKeySizes(String algorithm);

    /**
     * In non-FIPS mode: returns the algorithm unchanged.
     * In FIPS mode: returns the algorithm if it is approved for the given
     * service type, throws CryptoPolicyException otherwise.
     */
    String checkAlgorithm(String algorithm, ServiceType serviceType);

    /**
     * In non-FIPS mode: returns the algorithm unchanged.
     * In FIPS mode: returns the algorithm if both the algorithm and key size
     * are approved, throws CryptoPolicyException otherwise.
     */
    String checkAlgorithm(String algorithm, int keySize, ServiceType serviceType);

    boolean isFIPSMode();
}
```

## Implementation

### FIPS Detection

```java
@Override
public boolean isFIPSMode() {
    return PropsValues.FIPS_ENABLED;
}
```

### Algorithm Discovery (`@Activate`)

Iterates all installed providers and groups algorithms by `ServiceType`. No
provider-specific logic — in a FIPS JVM, the providers already expose only
FIPS-approved algorithms.

```java
private void _buildAlgorithmMap() {
    _algorithmMap = new EnumMap<>(ServiceType.class);

    for (Provider provider : Security.getProviders()) {
        for (Provider.Service service : provider.getServices()) {
            ServiceType serviceType = _SERVICE_TYPE_MAP.get(service.getType());

            if (serviceType != null) {
                _algorithmMap
                    .computeIfAbsent(serviceType, k -> new LinkedHashSet<>())
                    .add(service.getAlgorithm());
            }
        }
    }
}

private static final Map<String, ServiceType> _SERVICE_TYPE_MAP = Map.of(
    "Cipher",           ServiceType.CIPHER,
    "KeyGenerator",     ServiceType.KEY_GENERATOR,
    "KeyPairGenerator", ServiceType.KEY_PAIR_GENERATOR,
    "MessageDigest",    ServiceType.MESSAGE_DIGEST,
    "Mac",              ServiceType.MAC,
    "Signature",        ServiceType.SIGNATURE,
    "SecretKeyFactory", ServiceType.SECRET_KEY_FACTORY
);
```

### Key Size Discovery (`@Activate`)

Probes `KeyGenerator.init(size)` and `KeyPairGenerator.initialize(size)` for
candidate sizes, caching only the ones the provider accepts. In a FIPS JVM, the
provider rejects non-approved sizes with `InvalidParameterException`, so the map
naturally contains only valid sizes. No FIPS-specific logic needed.

```java
private static final int[] _SYMMETRIC_PROBE_SIZES =
    {40, 56, 64, 112, 128, 168, 192, 256, 512};

private static final int[] _ASYMMETRIC_PROBE_SIZES =
    {512, 1024, 2048, 3072, 4096};
```

### `checkAlgorithm`

```java
@Override
public String checkAlgorithm(String algorithm, ServiceType serviceType) {
    if (!PropsValues.FIPS_ENABLED) {
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

    if (PropsValues.FIPS_ENABLED &&
        !getAllowedKeySizes(algorithm).contains(keySize)) {

        throw new CryptoPolicyException(
            "Key size " + keySize + " for algorithm \"" + algorithm +
                "\" is not approved in FIPS mode");
    }

    return algorithm;
}
```

## Call Site Wiring

Each call site receives a `@Reference CryptoPolicyManager _cryptoPolicyManager`
and wraps its algorithm string before passing it to `getInstance()`.

| Module | Call site | Wiring |
|---|---|---|
| `portal-encryptor` | `Cipher.getInstance(algorithm)` | `checkAlgorithm(algorithm, KEY_SIZE, CIPHER)` |
| `portal-encryptor` | `KeyGenerator.getInstance(algorithm)` | `checkAlgorithm(algorithm, KEY_SIZE, KEY_GENERATOR)` |
| `portal-security-password-encryptor-impl` | `MessageDigest.getInstance("SHA-1")` | `checkAlgorithm("SHA-1", MESSAGE_DIGEST)` |
| `portal-crypto-hash-provider-message-digest` | `MessageDigest.getInstance(algorithm)` | `checkAlgorithm(algorithm, MESSAGE_DIGEST)` |
| `digital-signature-impl` | `Signature.getInstance("SHA256withRSA")` | `checkAlgorithm("SHA256withRSA", SIGNATURE)` |
| `multi-factor-authentication-timebased-otp-web` | `Mac.getInstance("HmacSHA1")` | `checkAlgorithm("HmacSHA1", MAC)` |
| `analytics-settings-impl` | `Signature.getInstance("DSA")` | `checkAlgorithm("DSA", SIGNATURE)` |
| `saml-opensaml-integration` | `KeyPairGenerator.getInstance(algorithm)` + `initialize(keySize)` | `checkAlgorithm(algorithm, keySize, KEY_PAIR_GENERATOR)` |

## Testing

### Unit Tests (`portal-security-crypto-policy-impl`)

Uses a standard `java.security.Provider` subclass (no FIPS-specific imports) to
simulate both environments:

- Non-FIPS: `checkAlgorithm` always returns the algorithm unchanged
- FIPS: `checkAlgorithm` returns approved algorithms, throws `CryptoPolicyException`
  for non-approved ones
- `getAllowedAlgorithms` returns only what the mock provider exposes
- `getAllowedKeySizes` returns only sizes the mock provider accepted during probing

### Integration Tests (requires real FIPS runtime)

- `getAllowedAlgorithms(MESSAGE_DIGEST)` must not contain `MD5` or `SHA-1`
- `getAllowedKeySizes("AES")` must equal `{128, 256}`
- `checkAlgorithm("MD5", MESSAGE_DIGEST)` must throw `CryptoPolicyException`
- `checkAlgorithm("SHA-256", MESSAGE_DIGEST)` must return `"SHA-256"`
- `checkAlgorithm("AES", 192, CIPHER)` must throw `CryptoPolicyException`
- `checkAlgorithm("AES", 256, CIPHER)` must return `"AES"`
