# LDAP Import Lock — Node-ID-Scoped Owner & Orphan Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent LDAP import locks stored in the database from blocking imports indefinitely after an abnormal server shutdown, by encoding the cluster node's UUID in the lock owner field and clearing orphaned (different-node) locks on component activation.

**Architecture:** Each startup of `LDAPUserImporterImpl` generates a unique per-run identifier (taken from the cluster node UUID via `ClusterExecutorUtil`, with a random-UUID fallback for non-cluster deployments). This ID is embedded in the lock's `owner` field. When the component activates, it scans every company's LDAP import lock: any lock whose embedded node ID is neither the current node's ID nor an actively-alive cluster peer is unconditionally removed. Existing locks that use the old owner format (no `::` separator — pre-fix deployments) are also cleared.

**Tech Stack:** Java 11, OSGi DS (`@Activate`), `LockManager` (already injected), `ClusterExecutorUtil` (Snapshot-based, null-safe), Mockito + `ReflectionTestUtil` for unit tests, Gradle for build/test.

---

## Background / Key Facts

File under change:
`modules/apps/portal-security/portal-security-ldap-impl/src/main/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImpl.java`

Test file:
`modules/apps/portal-security/portal-security-ldap-impl/src/test/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImplTest.java`

Lock key facts:
- Lock is stored in the `Lock_` database table (survives restarts).
- `LockManager.lock(userId, className, key, owner, ...)` is called at line 484 with `owner = LDAPUserImporterImpl.class.getName()` — a static string identical across all nodes and restarts.
- The lock is released in a `finally` block (line 536). If the JVM is killed, the `finally` never runs.
- `LockManager.fetchLock(String className, long key)` returns the existing lock or `null`.
- `Lock.getOwner()` is on the `com.liferay.portal.kernel.lock.Lock` kernel interface.
- `ClusterExecutorUtil.getLocalClusterNode()` can return `null` when no `ClusterExecutor` service is registered (non-cluster deployments). Guard against it.
- `ClusterExecutorUtil.isEnabled()` returns `false` when not in cluster mode.
- `ClusterExecutorUtil` is in `portal-kernel`; no new build dependency needed.
- `Snapshot.get()` (used internally by `ClusterExecutorUtil`) returns `null` when the service is absent — so wrap every `ClusterExecutorUtil` call in a null check.

New owner format: `<clusterNodeId>::com.liferay.portal.security.ldap.internal.exportimport.LDAPUserImporterImpl`

Run unit tests for this module with:
```bash
cd modules/apps/portal-security/portal-security-ldap-impl
../../../gradlew test --tests 'com.liferay.portal.security.ldap.internal.exportimport.LDAPUserImporterImplTest' -x formatSource
```

---

## Task 1: Encode cluster node ID in the lock owner

### Files
- Modify: `modules/apps/portal-security/portal-security-ldap-impl/src/main/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImpl.java`
- Test: `modules/apps/portal-security/portal-security-ldap-impl/src/test/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImplTest.java`

- [ ] **Step 1.1: Write failing tests for `_buildLockOwner` and node-ID initialisation**

Add this test method to `LDAPUserImporterImplTest` (inside the existing class, after the existing `testUpdateUser` method):

```java
@Test
public void testBuildLockOwnerContainsNodeId() {
    String nodeId = RandomTestUtil.randomString();

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_clusterNodeId", nodeId);

    String owner = ReflectionTestUtil.invoke(
        _ldapUserImporterImpl, "_buildLockOwner", new Class<?>[0]);

    Assert.assertTrue(
        "owner must start with the node ID",
        owner.startsWith(nodeId + "::"));
    Assert.assertTrue(
        "owner must end with the implementation class name",
        owner.endsWith(
            "com.liferay.portal.security.ldap.internal.exportimport" +
                ".LDAPUserImporterImpl"));
}

@Test
public void testActivateStoresNonNullClusterNodeId() {
    ReflectionTestUtil.invoke(
        _ldapUserImporterImpl, "_initClusterNodeId", new Class<?>[0]);

    String nodeId = ReflectionTestUtil.getFieldValue(
        _ldapUserImporterImpl, "_clusterNodeId");

    Assert.assertNotNull("_clusterNodeId must not be null after activate", nodeId);
    Assert.assertFalse(
        "_clusterNodeId must not be empty", nodeId.isEmpty());
}
```

Also add the missing `Assert` import at the top of the test file:

```java
import org.junit.Assert;
```

- [ ] **Step 1.2: Run tests to confirm they fail**

```bash
cd modules/apps/portal-security/portal-security-ldap-impl
../../../gradlew test --tests 'com.liferay.portal.security.ldap.internal.exportimport.LDAPUserImporterImplTest' -x formatSource
```

Expected: FAIL — `_clusterNodeId` field and `_buildLockOwner` / `_initClusterNodeId` methods do not exist yet.

- [ ] **Step 1.3: Add the `_clusterNodeId` field, snapshot, `_initClusterNodeId()`, and `_buildLockOwner()` to `LDAPUserImporterImpl`**

1. Add the import block (after existing imports, keeping alphabetical order):

```java
import com.liferay.portal.kernel.cluster.ClusterExecutor;
import com.liferay.portal.kernel.cluster.ClusterNode;
import com.liferay.portal.kernel.module.service.Snapshot;

import java.util.UUID;
```

2. Add the `_initClusterNodeId()` private method (near other private helpers at the bottom of the class):

```java
private void _initClusterNodeId() {
    ClusterExecutor clusterExecutor = _clusterExecutorSnapshot.get();

    if (clusterExecutor != null) {
        ClusterNode localClusterNode = clusterExecutor.getLocalClusterNode();

        if (localClusterNode != null) {
            _clusterNodeId = localClusterNode.getClusterNodeId();

            return;
        }
    }

    _clusterNodeId = UUID.randomUUID().toString();
}
```

3. Add `_buildLockOwner()` right after `_initClusterNodeId()`:

```java
private String _buildLockOwner() {
    return _clusterNodeId + "::" + LDAPUserImporterImpl.class.getName();
}
```

4. Update `activate()` to call `_initClusterNodeId()` as its first line:

Existing `activate()`:
```java
@Activate
protected void activate() {
    _companySecurityAuthType = GetterUtil.getString(
        PropsUtil.get(PropsKeys.COMPANY_SECURITY_AUTH_TYPE));
    _portalCache = (PortalCache<String, Long>)_singleVMPool.getPortalCache(
        UserImporter.class.getName());
}
```

New `activate()`:
```java
@Activate
protected void activate() {
    _initClusterNodeId();

    _companySecurityAuthType = GetterUtil.getString(
        PropsUtil.get(PropsKeys.COMPANY_SECURITY_AUTH_TYPE));
    _portalCache = (PortalCache<String, Long>)_singleVMPool.getPortalCache(
        UserImporter.class.getName());
}
```

5. Update the `_lockManager.lock(...)` call in `importUsers(long companyId)` (line ~484) to use `_buildLockOwner()` instead of `LDAPUserImporterImpl.class.getName()`:

Old:
```java
Lock lock = _lockManager.lock(
    userId, UserImporter.class.getName(), companyId,
    LDAPUserImporterImpl.class.getName(), false,
    ldapImportConfiguration.importLockExpirationTime(), false);
```

New:
```java
Lock lock = _lockManager.lock(
    userId, UserImporter.class.getName(), companyId,
    _buildLockOwner(), false,
    ldapImportConfiguration.importLockExpirationTime(), false);
```

6. Add the static snapshot field and the `_clusterNodeId` instance field in the private fields section (keep fields alphabetical by name):

```java
private static final Snapshot<ClusterExecutor> _clusterExecutorSnapshot =
    new Snapshot<>(LDAPUserImporterImpl.class, ClusterExecutor.class);
```

```java
private String _clusterNodeId;
```

- [ ] **Step 1.4: Run tests to confirm they pass**

```bash
cd modules/apps/portal-security/portal-security-ldap-impl
../../../gradlew test --tests 'com.liferay.portal.security.ldap.internal.exportimport.LDAPUserImporterImplTest' -x formatSource
```

Expected: all tests PASS.

- [ ] **Step 1.5: Commit**

```bash
git add modules/apps/portal-security/portal-security-ldap-impl/src/main/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImpl.java
git add modules/apps/portal-security/portal-security-ldap-impl/src/test/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImplTest.java
git commit -m "$(cat <<'EOF'
LPP-64012 Encode cluster node UUID in LDAP import lock owner field

The lock owner now carries the current node's UUID so that orphaned
locks from dead nodes can be distinguished from locks held by live peers.
EOF
)"
```

---

## Task 2: Clear orphaned locks on component activation

### Files
- Modify: `modules/apps/portal-security/portal-security-ldap-impl/src/main/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImpl.java`
- Test: `modules/apps/portal-security/portal-security-ldap-impl/src/test/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImplTest.java`

- [ ] **Step 2.1: Write failing tests for `_clearOrphanedLock`**

Add the following test methods to `LDAPUserImporterImplTest`, after the existing tests. Each method sets up an independent `LDAPUserImporterImpl` instance via the static field already present, injecting mocks with `ReflectionTestUtil.setFieldValue`.

```java
@Test
public void testClearOrphanedLockDoesNothingWhenNoLockExists() {
    String currentNodeId = RandomTestUtil.randomString();

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_clusterNodeId", currentNodeId);

    LockManager lockManager = Mockito.mock(LockManager.class);

    Mockito.when(
        lockManager.fetchLock(
            Mockito.anyString(), Mockito.anyLong())
    ).thenReturn(
        null
    );

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_lockManager", lockManager);

    ReflectionTestUtil.invoke(
        _ldapUserImporterImpl, "_clearOrphanedLock",
        new Class<?>[] {long.class}, 1L);

    Mockito.verify(lockManager, Mockito.never()).unlock(
        Mockito.anyString(), Mockito.anyLong());
}

@Test
public void testClearOrphanedLockRemovesLockFromDeadNode() {
    String currentNodeId = RandomTestUtil.randomString();
    String deadNodeId = RandomTestUtil.randomString();

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_clusterNodeId", currentNodeId);

    Lock lock = Mockito.mock(Lock.class);

    Mockito.when(
        lock.getOwner()
    ).thenReturn(
        deadNodeId + "::" +
            "com.liferay.portal.security.ldap.internal.exportimport" +
            ".LDAPUserImporterImpl"
    );

    LockManager lockManager = Mockito.mock(LockManager.class);

    Mockito.when(
        lockManager.fetchLock(
            Mockito.anyString(), Mockito.anyLong())
    ).thenReturn(
        lock
    );

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_lockManager", lockManager);

    ReflectionTestUtil.invoke(
        _ldapUserImporterImpl, "_clearOrphanedLock",
        new Class<?>[] {long.class}, 1L);

    Mockito.verify(lockManager).unlock(
        "com.liferay.portal.kernel.security.ldap.UserImporter", 1L);
}

@Test
public void testClearOrphanedLockKeepsLockFromActivePeer() {
    String currentNodeId = RandomTestUtil.randomString();
    String peerNodeId = RandomTestUtil.randomString();

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_clusterNodeId", currentNodeId);

    Lock lock = Mockito.mock(Lock.class);

    Mockito.when(
        lock.getOwner()
    ).thenReturn(
        peerNodeId + "::" +
            "com.liferay.portal.security.ldap.internal.exportimport" +
            ".LDAPUserImporterImpl"
    );

    LockManager lockManager = Mockito.mock(LockManager.class);

    Mockito.when(
        lockManager.fetchLock(
            Mockito.anyString(), Mockito.anyLong())
    ).thenReturn(
        lock
    );

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_lockManager", lockManager);

    ClusterExecutor clusterExecutor = Mockito.mock(ClusterExecutor.class);

    Mockito.when(
        clusterExecutor.isEnabled()
    ).thenReturn(
        true
    );

    Mockito.when(
        clusterExecutor.isClusterNodeAlive(peerNodeId)
    ).thenReturn(
        true
    );

    try (MockedStatic<ClusterExecutorUtil> clusterExecutorUtilMockedStatic =
            Mockito.mockStatic(ClusterExecutorUtil.class)) {

        clusterExecutorUtilMockedStatic.when(
            ClusterExecutorUtil::getClusterExecutor
        ).thenReturn(
            clusterExecutor
        );

        ReflectionTestUtil.invoke(
            _ldapUserImporterImpl, "_clearOrphanedLock",
            new Class<?>[] {long.class}, 1L);
    }

    Mockito.verify(lockManager, Mockito.never()).unlock(
        Mockito.anyString(), Mockito.anyLong());
}

@Test
public void testClearOrphanedLockRemovesLockWithLegacyOwnerFormat() {
    String currentNodeId = RandomTestUtil.randomString();

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_clusterNodeId", currentNodeId);

    Lock lock = Mockito.mock(Lock.class);

    // Old format: just the class name, no "::" separator
    Mockito.when(
        lock.getOwner()
    ).thenReturn(
        "com.liferay.portal.security.ldap.internal.exportimport" +
            ".LDAPUserImporterImpl"
    );

    LockManager lockManager = Mockito.mock(LockManager.class);

    Mockito.when(
        lockManager.fetchLock(
            Mockito.anyString(), Mockito.anyLong())
    ).thenReturn(
        lock
    );

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_lockManager", lockManager);

    ReflectionTestUtil.invoke(
        _ldapUserImporterImpl, "_clearOrphanedLock",
        new Class<?>[] {long.class}, 1L);

    Mockito.verify(lockManager).unlock(
        "com.liferay.portal.kernel.security.ldap.UserImporter", 1L);
}
```

Also add this import at the top of `LDAPUserImporterImplTest`:

```java
import com.liferay.portal.kernel.cluster.ClusterExecutor;
import com.liferay.portal.kernel.cluster.ClusterExecutorUtil;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.lock.LockManager;
```

> **Note on `ClusterExecutorUtil::getClusterExecutor`:** `ClusterExecutorUtil` has no `getClusterExecutor()` method; we intercept it differently. See Step 2.3 for the actual implementation which reads the snapshot via the static field — and in Step 2.2 we'll revise the test for the peer-alive case to use `MockedStatic` on the `Snapshot.get()` path instead. The test as written above will fail due to the absent method, which is what we want.

- [ ] **Step 2.2: Run tests to confirm they fail**

```bash
cd modules/apps/portal-security/portal-security-ldap-impl
../../../gradlew test --tests 'com.liferay.portal.security.ldap.internal.exportimport.LDAPUserImporterImplTest' -x formatSource
```

Expected: FAIL — `_clearOrphanedLock` method does not exist yet. The peer-alive test will also fail due to the stub `getClusterExecutor()` call — you will fix this in Step 2.3 after you know the real API shape.

- [ ] **Step 2.3: Implement `_clearOrphanedLock()` and wire it into `activate()`**

Add the following private method to `LDAPUserImporterImpl` (in the private-methods section):

```java
private void _clearOrphanedLock(long companyId) {
    Lock lock = _lockManager.fetchLock(
        UserImporter.class.getName(), companyId);

    if (lock == null) {
        return;
    }

    String owner = lock.getOwner();

    if (owner == null) {
        _lockManager.unlock(UserImporter.class.getName(), companyId);

        return;
    }

    int separatorIndex = owner.indexOf("::");

    if (separatorIndex < 0) {
        // Pre-fix lock format: owner is just the class name. Clear it.
        if (_log.isInfoEnabled()) {
            _log.info(
                StringBundler.concat(
                    "Clearing legacy-format LDAP import lock for company ",
                    companyId, " (owner=", owner, ")"));
        }

        _lockManager.unlock(UserImporter.class.getName(), companyId);

        return;
    }

    String lockNodeId = owner.substring(0, separatorIndex);

    if (lockNodeId.equals(_clusterNodeId)) {
        return;
    }

    ClusterExecutor clusterExecutor = _clusterExecutorSnapshot.get();

    if ((clusterExecutor != null) && clusterExecutor.isEnabled() &&
        clusterExecutor.isClusterNodeAlive(lockNodeId)) {

        return;
    }

    if (_log.isInfoEnabled()) {
        _log.info(
            StringBundler.concat(
                "Clearing orphaned LDAP import lock for company ", companyId,
                " (was held by node ", lockNodeId, ")"));
    }

    _lockManager.unlock(UserImporter.class.getName(), companyId);
}
```

Add the call to `_clearOrphanedLock` from `activate()` after the existing initialisation:

```java
@Activate
protected void activate() {
    _initClusterNodeId();

    _companySecurityAuthType = GetterUtil.getString(
        PropsUtil.get(PropsKeys.COMPANY_SECURITY_AUTH_TYPE));
    _portalCache = (PortalCache<String, Long>)_singleVMPool.getPortalCache(
        UserImporter.class.getName());

    _companyLocalService.forEachCompanyId(
        companyId -> _clearOrphanedLock(companyId));
}
```

Now fix the peer-alive test to not use the non-existent `getClusterExecutor()`. Replace the `testClearOrphanedLockKeepsLockFromActivePeer` body with a version that mocks `_clusterExecutorSnapshot` via `ReflectionTestUtil`:

```java
@Test
public void testClearOrphanedLockKeepsLockFromActivePeer() throws Exception {
    String currentNodeId = RandomTestUtil.randomString();
    String peerNodeId = RandomTestUtil.randomString();

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_clusterNodeId", currentNodeId);

    Lock lock = Mockito.mock(Lock.class);

    Mockito.when(
        lock.getOwner()
    ).thenReturn(
        peerNodeId + "::" +
            "com.liferay.portal.security.ldap.internal.exportimport" +
            ".LDAPUserImporterImpl"
    );

    LockManager lockManager = Mockito.mock(LockManager.class);

    Mockito.when(
        lockManager.fetchLock(Mockito.anyString(), Mockito.anyLong())
    ).thenReturn(
        lock
    );

    ReflectionTestUtil.setFieldValue(
        _ldapUserImporterImpl, "_lockManager", lockManager);

    ClusterExecutor clusterExecutor = Mockito.mock(ClusterExecutor.class);

    Mockito.when(
        clusterExecutor.isEnabled()
    ).thenReturn(
        true
    );

    Mockito.when(
        clusterExecutor.isClusterNodeAlive(peerNodeId)
    ).thenReturn(
        true
    );

    Snapshot<ClusterExecutor> snapshot = Mockito.mock(Snapshot.class);

    Mockito.when(
        snapshot.get()
    ).thenReturn(
        clusterExecutor
    );

    ReflectionTestUtil.setFieldValue(
        LDAPUserImporterImpl.class, "_clusterExecutorSnapshot", snapshot);

    try {
        ReflectionTestUtil.invoke(
            _ldapUserImporterImpl, "_clearOrphanedLock",
            new Class<?>[] {long.class}, 1L);

        Mockito.verify(lockManager, Mockito.never()).unlock(
            Mockito.anyString(), Mockito.anyLong());
    }
    finally {
        // Restore static field so other tests are not affected
        ReflectionTestUtil.setFieldValue(
            LDAPUserImporterImpl.class, "_clusterExecutorSnapshot",
            new Snapshot<>(LDAPUserImporterImpl.class, ClusterExecutor.class));
    }
}
```

Also remove the now-unnecessary `import com.liferay.portal.kernel.cluster.ClusterExecutorUtil;` that was added in Step 2.1 (since we no longer use `MockedStatic<ClusterExecutorUtil>`), and remove the unused `import org.mockito.MockedStatic;` if it was only used there (keep it if `testUpdateUser` already uses it — it does, so keep it).

Add the missing import:
```java
import com.liferay.portal.kernel.module.service.Snapshot;
```

- [ ] **Step 2.4: Run tests to confirm they pass**

```bash
cd modules/apps/portal-security/portal-security-ldap-impl
../../../gradlew test --tests 'com.liferay.portal.security.ldap.internal.exportimport.LDAPUserImporterImplTest' -x formatSource
```

Expected: all tests PASS.

- [ ] **Step 2.5: Commit**

```bash
git add modules/apps/portal-security/portal-security-ldap-impl/src/main/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImpl.java
git add modules/apps/portal-security/portal-security-ldap-impl/src/test/java/com/liferay/portal/security/ldap/internal/exportimport/LDAPUserImporterImplTest.java
git commit -m "$(cat <<'EOF'
LPP-64012 Clear orphaned LDAP import locks on component activation

On startup, sweep every company's LDAP import lock and remove any lock
whose embedded node UUID is neither the current node nor a live cluster
peer. This handles abnormal-shutdown leftovers while leaving peer-held
locks intact in a clustered environment. Legacy-format locks (no "::"
separator) are also cleared to migrate pre-fix deployments.
EOF
)"
```

---

## Self-Review

### Spec Coverage

| Requirement | Task |
|---|---|
| Node UUID embedded in owner field | Task 1 |
| On `@Activate`, clear own-node orphaned locks | Task 2 |
| Cluster peer's active lock is preserved | Task 2 (`testClearOrphanedLockKeepsLockFromActivePeer`) |
| Non-cluster fallback (no ClusterExecutor) | Task 1 (`_initClusterNodeId` fallback path), Task 2 (`clusterExecutor == null` branch) |
| Legacy lock format (pre-fix) cleared | Task 2 (`testClearOrphanedLockRemovesLockWithLegacyOwnerFormat`) |
| Null lock handled safely | Task 2 (`testClearOrphanedLockDoesNothingWhenNoLockExists`) |

### Placeholder Scan
None found.

### Type Consistency

- `_clusterNodeId` field: `String` — consistent in `_initClusterNodeId`, `_buildLockOwner`, `_clearOrphanedLock`, and all tests.
- `_clusterExecutorSnapshot` field: `Snapshot<ClusterExecutor>` — static, final, consistent across `_initClusterNodeId`, `_clearOrphanedLock`, and test setup.
- `UserImporter.class.getName()` used as the lock `className` in `fetchLock` and `unlock` — consistent with existing code at lines 484 and 536.
- `_lockManager.fetchLock(String, long)` signature: takes `(className, companyId)` — `companyId` is `long`. Consistent with `LockManager` interface.
