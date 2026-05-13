/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.ldap.internal.exportimport;

import com.liferay.portal.kernel.cluster.ClusterExecutor;
import com.liferay.portal.kernel.lock.Lock;
import com.liferay.portal.kernel.lock.LockManager;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.security.ldap.LDAPSettings;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.security.ldap.configuration.ConfigurationProvider;
import com.liferay.portal.security.ldap.configuration.LDAPServerConfiguration;
import com.liferay.portal.security.ldap.exportimport.LDAPUser;
import com.liferay.portal.security.ldap.exportimport.configuration.LDAPImportConfiguration;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Date;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jorge Díaz
 * @author Manuele Castro
 */
public class LDAPUserImporterImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testUpdateUser() throws Exception {
		ConfigurationProvider<LDAPImportConfiguration>
			ldapImportConfigurationProvider = Mockito.mock(
				ConfigurationProvider.class);

		Mockito.when(
			ldapImportConfigurationProvider.getConfiguration(Mockito.anyLong())
		).thenReturn(
			Mockito.mock(LDAPImportConfiguration.class)
		);

		ReflectionTestUtil.setFieldValue(
			_ldapUserImporterImpl, "_ldapImportConfigurationProvider",
			ldapImportConfigurationProvider);

		ConfigurationProvider<LDAPServerConfiguration>
			ldapServerConfigurationProvider = Mockito.mock(
				ConfigurationProvider.class);

		LDAPServerConfiguration ldapServerConfiguration = Mockito.mock(
			LDAPServerConfiguration.class);

		String modifiedDateString = "Thu Apr 2 19:18:33 GMT 2026";

		Mockito.when(
			ldapServerConfiguration.modifiedDate()
		).thenReturn(
			modifiedDateString
		);

		Mockito.when(
			ldapServerConfigurationProvider.getConfiguration(
				Mockito.anyLong(), Mockito.anyLong())
		).thenReturn(
			ldapServerConfiguration
		);

		ReflectionTestUtil.setFieldValue(
			_ldapUserImporterImpl, "_ldapServerConfigurationProvider",
			ldapServerConfigurationProvider);

		ReflectionTestUtil.setFieldValue(
			_ldapUserImporterImpl, "_ldapSettings",
			Mockito.mock(LDAPSettings.class));

		LDAPImportContext ldapImportContext = Mockito.mock(
			LDAPImportContext.class);

		Mockito.when(
			ldapImportContext.getCompanyId()
		).thenReturn(
			RandomTestUtil.randomLong()
		);

		Mockito.when(
			ldapImportContext.getLdapServerId()
		).thenReturn(
			RandomTestUtil.randomLong()
		);

		LDAPUser ldapUser = new LDAPUser();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setLanguageId(LocaleUtil.BRAZIL.toLanguageTag());

		ldapUser.setServiceContext(serviceContext);

		User user = Mockito.mock(User.class);

		Mockito.when(
			user.getModifiedDate()
		).thenReturn(
			new Date()
		);

		try (MockedStatic<DateUtil> dateUtilMockedStatic = Mockito.mockStatic(
				DateUtil.class, Mockito.CALLS_REAL_METHODS)) {

			ReflectionTestUtil.invoke(
				_ldapUserImporterImpl, "_updateUser",
				new Class<?>[] {
					LDAPImportContext.class, LDAPUser.class, User.class,
					String.class, String.class, boolean.class
				},
				ldapImportContext, ldapUser, user,
				RandomTestUtil.randomString(),
				String.valueOf(RandomTestUtil.randomLong()), false);

			dateUtilMockedStatic.verify(
				() -> DateUtil.parseDate(
					Mockito.eq("EEE MMM d HH:mm:ss zzz yyyy"),
					Mockito.eq(modifiedDateString), Mockito.eq(LocaleUtil.US)),
				Mockito.times(1));
		}
	}

	@Test
	public void testActivateStoresNonNullClusterNodeId() {
		ReflectionTestUtil.invoke(
			_ldapUserImporterImpl, "_initClusterNodeId", new Class<?>[0]);

		String nodeId = ReflectionTestUtil.getFieldValue(
			_ldapUserImporterImpl, "_clusterNodeId");

		Assert.assertNotNull("cluster node ID must be set after activation", nodeId);
		Assert.assertFalse(
			"cluster node ID must not be empty after activation", nodeId.isEmpty());
	}

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
			owner.endsWith(LDAPUserImporterImpl.class.getName()));
	}

	@Test
	public void testClearOrphanedLockDoesNothingWhenNoLockExists() {
		String currentNodeId = RandomTestUtil.randomString();

		ReflectionTestUtil.setFieldValue(
			_ldapUserImporterImpl, "_clusterNodeId", currentNodeId);

		LockManager lockManager = Mockito.mock(LockManager.class);

		Mockito.when(
			lockManager.fetchLock(Mockito.anyString(), Mockito.anyLong())
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
			deadNodeId + "::" + LDAPUserImporterImpl.class.getName()
		);

		LockManager lockManager = Mockito.mock(LockManager.class);

		Mockito.when(
			lockManager.fetchLock(Mockito.anyString(), Mockito.anyLong())
		).thenReturn(
			lock
		);

		ReflectionTestUtil.setFieldValue(
			_ldapUserImporterImpl, "_lockManager", lockManager);

		ReflectionTestUtil.invoke(
			_ldapUserImporterImpl, "_clearOrphanedLock",
			new Class<?>[] {long.class}, 1L);

		Mockito.verify(lockManager).unlock(
			"com.liferay.portal.security.exportimport.UserImporter", 1L);
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
			peerNodeId + "::" + LDAPUserImporterImpl.class.getName()
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
			ReflectionTestUtil.setFieldValue(
				LDAPUserImporterImpl.class, "_clusterExecutorSnapshot",
				new Snapshot<>(LDAPUserImporterImpl.class, ClusterExecutor.class));
		}
	}

	@Test
	public void testClearOrphanedLockRemovesLockWithLegacyOwnerFormat() {
		String currentNodeId = RandomTestUtil.randomString();

		ReflectionTestUtil.setFieldValue(
			_ldapUserImporterImpl, "_clusterNodeId", currentNodeId);

		Lock lock = Mockito.mock(Lock.class);

		Mockito.when(
			lock.getOwner()
		).thenReturn(
			LDAPUserImporterImpl.class.getName()
		);

		LockManager lockManager = Mockito.mock(LockManager.class);

		Mockito.when(
			lockManager.fetchLock(Mockito.anyString(), Mockito.anyLong())
		).thenReturn(
			lock
		);

		ReflectionTestUtil.setFieldValue(
			_ldapUserImporterImpl, "_lockManager", lockManager);

		ReflectionTestUtil.invoke(
			_ldapUserImporterImpl, "_clearOrphanedLock",
			new Class<?>[] {long.class}, 1L);

		Mockito.verify(lockManager).unlock(
			"com.liferay.portal.security.exportimport.UserImporter", 1L);
	}

	private static final LDAPUserImporterImpl _ldapUserImporterImpl =
		new LDAPUserImporterImpl();

}