/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.audit.storage.service.test;

import com.liferay.account.constants.AccountConstants;
import com.liferay.account.model.AccountEntry;
import com.liferay.account.service.AccountEntryLocalService;
import com.liferay.account.service.AccountEntryUserRelLocalService;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.audit.AuditMessage;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.security.audit.storage.model.AuditEvent;
import com.liferay.portal.security.audit.storage.service.AuditEventLocalService;
import com.liferay.portal.security.audit.storage.service.AuditEventService;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class AuditEventAccountScopingTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		BundleContext bundleContext = FrameworkUtil.getBundle(
			AuditEventAccountScopingTest.class
		).getBundleContext();

		_accountEntryLocalService = bundleContext.getService(
			bundleContext.getServiceReference(AccountEntryLocalService.class));
		_accountEntryUserRelLocalService = bundleContext.getService(
			bundleContext.getServiceReference(
				AccountEntryUserRelLocalService.class));
		_auditEventLocalService = bundleContext.getService(
			bundleContext.getServiceReference(AuditEventLocalService.class));
		_auditEventService = bundleContext.getService(
			bundleContext.getServiceReference(AuditEventService.class));

		_companyId = PortalUtil.getDefaultCompanyId();
		_eventType = "TEST_" + RandomTestUtil.randomString();

		_accountEntry = _addAccountEntry();
		_otherAccountEntry = _addAccountEntry();

		_accountUser = UserTestUtil.addUser();

		_accountEntryUserRelLocalService.addAccountEntryUserRel(
			_accountEntry.getAccountEntryId(), _accountUser.getUserId());

		_previousPermissionChecker =
			PermissionThreadLocal.getPermissionChecker();
	}

	@After
	public void tearDown() throws Exception {
		PermissionThreadLocal.setPermissionChecker(_previousPermissionChecker);

		for (AuditEvent auditEvent : _auditEvents) {
			_auditEventLocalService.deleteAuditEvent(auditEvent);
		}

		if (_accountEntry != null) {
			_accountEntryUserRelLocalService.
				deleteAccountEntryUserRelsByAccountEntryId(
					_accountEntry.getAccountEntryId());

			_accountEntryLocalService.deleteAccountEntry(_accountEntry);
		}

		if (_otherAccountEntry != null) {
			_accountEntryLocalService.deleteAccountEntry(_otherAccountEntry);
		}
	}

	@Test
	public void testAccountMemberCanReadOwnAccountEvents() throws Exception {
		long accountEntryId = _accountEntry.getAccountEntryId();

		_addAuditEvent(accountEntryId, null);

		_setPermissionChecker(_accountUser);

		List<AuditEvent> auditEvents = _auditEventService.getAuditEvents(
			_companyId, new long[] {accountEntryId}, 0, 0, null, null, null,
			null, _eventType, null, null, null, null, null, 0, null, true, 0,
			Integer.MAX_VALUE);

		Assert.assertFalse(auditEvents.isEmpty());

		for (AuditEvent auditEvent : auditEvents) {
			Assert.assertEquals(accountEntryId, auditEvent.getAccountEntryId());
		}
	}

	@Test(expected = PrincipalException.MustHavePermission.class)
	public void testAccountMemberRequestingForbiddenAccountThrows()
		throws Exception {

		_addAuditEvent(_otherAccountEntry.getAccountEntryId(), null);

		_setPermissionChecker(_accountUser);

		_auditEventService.getAuditEvents(
			_companyId, new long[] {_otherAccountEntry.getAccountEntryId()}, 0,
			0, null, null, null, null, _eventType, null, null, null, null, null,
			0, null, true, 0, Integer.MAX_VALUE);
	}

	@Test
	public void testAuditEventWithAccountContextStoresAccountEntryId()
		throws Exception {

		long accountEntryId = _accountEntry.getAccountEntryId();

		AuditEvent auditEvent = _addAuditEvent(accountEntryId, null);

		Assert.assertEquals(accountEntryId, auditEvent.getAccountEntryId());
	}

	@Test
	public void testAuditEventWithoutAccountContextStoresZero()
		throws Exception {

		AuditEvent auditEvent = _addAuditEvent(
			AccountConstants.ACCOUNT_ENTRY_ID_DEFAULT, null);

		Assert.assertEquals(0, auditEvent.getAccountEntryId());
	}

	@Test
	public void testCompanyAdminCanReadAcrossAccounts() throws Exception {
		long accountEntryId = _accountEntry.getAccountEntryId();
		long otherAccountEntryId = _otherAccountEntry.getAccountEntryId();

		_addAuditEvent(accountEntryId, null);
		_addAuditEvent(otherAccountEntryId, null);

		_setPermissionChecker(TestPropsValues.getUser());

		List<AuditEvent> auditEvents = _auditEventService.getAuditEvents(
			_companyId, new long[] {accountEntryId, otherAccountEntryId}, 0, 0,
			null, null, null, null, _eventType, null, null, null, null, null, 0,
			null, true, 0, Integer.MAX_VALUE);

		Assert.assertEquals(auditEvents.toString(), 2, auditEvents.size());
	}

	@Test
	public void testGetAuditEventsByAccountEntryIds() throws Exception {
		long accountEntryId = _accountEntry.getAccountEntryId();
		long otherAccountEntryId = _otherAccountEntry.getAccountEntryId();

		_addAuditEvent(accountEntryId, null);
		_addAuditEvent(accountEntryId, null);
		_addAuditEvent(otherAccountEntryId, null);

		List<AuditEvent> auditEvents = _auditEventLocalService.getAuditEvents(
			_companyId, new long[] {accountEntryId}, 0, 0, null, null, null,
			null, _eventType, null, null, null, null, null, 0, null, true, 0,
			Integer.MAX_VALUE);

		Assert.assertEquals(auditEvents.toString(), 2, auditEvents.size());

		for (AuditEvent auditEvent : auditEvents) {
			Assert.assertEquals(accountEntryId, auditEvent.getAccountEntryId());
		}
	}

	@Test
	public void testGetAuditEventsByMultipleAccountEntryIds() throws Exception {
		long accountEntryId = _accountEntry.getAccountEntryId();
		long otherAccountEntryId = _otherAccountEntry.getAccountEntryId();

		_addAuditEvent(accountEntryId, null);
		_addAuditEvent(otherAccountEntryId, null);
		_addAuditEvent(AccountConstants.ACCOUNT_ENTRY_ID_DEFAULT, null);

		List<AuditEvent> auditEvents = _auditEventLocalService.getAuditEvents(
			_companyId, new long[] {accountEntryId, otherAccountEntryId}, 0, 0,
			null, null, null, null, _eventType, null, null, null, null, null, 0,
			null, true, 0, Integer.MAX_VALUE);

		Assert.assertEquals(auditEvents.toString(), 2, auditEvents.size());
	}

	@Test
	public void testGetAuditEventsByScope() throws Exception {
		_addAuditEvent(0, "ai-hub");
		_addAuditEvent(0, "ai-hub");
		_addAuditEvent(0, "fips");

		List<AuditEvent> auditEvents = _auditEventLocalService.getAuditEvents(
			_companyId, null, 0, 0, null, "ai-hub", null, null, _eventType,
			null, null, null, null, null, 0, null, true, 0, Integer.MAX_VALUE);

		Assert.assertEquals(auditEvents.toString(), 2, auditEvents.size());

		for (AuditEvent auditEvent : auditEvents) {
			Assert.assertEquals("ai-hub", auditEvent.getScope());
		}
	}

	@Test
	public void testScopeStoredAsNullByDefault() throws Exception {
		AuditEvent auditEvent = _addAuditEvent(0, null);

		Assert.assertNull(auditEvent.getScope());
	}

	@Test
	public void testScopeStoredCorrectly() throws Exception {
		AuditEvent auditEvent = _addAuditEvent(0, "ai-hub");

		Assert.assertEquals("ai-hub", auditEvent.getScope());
	}

	private AccountEntry _addAccountEntry() throws Exception {
		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext();

		return _accountEntryLocalService.addAccountEntry(
			RandomTestUtil.randomString(), serviceContext.getUserId(), 0,
			RandomTestUtil.randomString(), RandomTestUtil.randomString(), null,
			RandomTestUtil.randomString() + "@liferay.com", null, null,
			AccountConstants.ACCOUNT_ENTRY_TYPE_BUSINESS,
			WorkflowConstants.STATUS_APPROVED, serviceContext);
	}

	private AuditEvent _addAuditEvent(long accountEntryId, String scope)
		throws Exception {

		AuditMessage auditMessage = new AuditMessage(
			_eventType, _companyId, TestPropsValues.getUserId(), "TestUser");

		auditMessage.setAccountEntryId(accountEntryId);
		auditMessage.setScope(scope);

		AuditEvent auditEvent = _auditEventLocalService.addAuditEvent(
			auditMessage);

		_auditEvents.add(auditEvent);

		return auditEvent;
	}

	private void _setPermissionChecker(User user) throws Exception {
		PermissionThreadLocal.setPermissionChecker(
			PermissionCheckerFactoryUtil.create(user));
	}

	private AccountEntry _accountEntry;
	private AccountEntryLocalService _accountEntryLocalService;
	private AccountEntryUserRelLocalService _accountEntryUserRelLocalService;
	private User _accountUser;
	private AuditEventLocalService _auditEventLocalService;
	private final List<AuditEvent> _auditEvents = new ArrayList<>();
	private AuditEventService _auditEventService;
	private long _companyId;
	private String _eventType;
	private AccountEntry _otherAccountEntry;
	private PermissionChecker _previousPermissionChecker;

}