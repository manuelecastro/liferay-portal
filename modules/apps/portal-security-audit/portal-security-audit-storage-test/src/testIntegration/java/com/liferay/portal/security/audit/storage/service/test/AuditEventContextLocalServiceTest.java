/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.audit.storage.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.security.audit.storage.model.AuditEvent;
import com.liferay.portal.security.audit.storage.service.AuditEventLocalService;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Date;
import java.util.List;

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
public class AuditEventContextLocalServiceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		BundleContext bundleContext = FrameworkUtil.getBundle(
			AuditEventContextLocalServiceTest.class
		).getBundleContext();

		_auditEventLocalService = bundleContext.getService(
			bundleContext.getServiceReference(AuditEventLocalService.class));

		_companyId = PortalUtil.getDefaultCompanyId();
		_eventType = "TEST_" + RandomTestUtil.randomString();

		_addAuditEvent("product-catalog");
		_addAuditEvent("product-catalog");
		_addAuditEvent("order-management");
	}

	@Test
	public void testFilterByContext() throws Exception {
		List<AuditEvent> auditEvents = _auditEventLocalService.getAuditEvents(
			_companyId, null, 0, 0, null, null, null, _eventType, null, null,
			null, null, null, 0, null, "product-catalog", true, 0,
			Integer.MAX_VALUE);

		Assert.assertEquals(auditEvents.toString(), 2, auditEvents.size());

		for (AuditEvent auditEvent : auditEvents) {
			Assert.assertEquals("product-catalog", auditEvent.getContext());
		}
	}

	@Test
	public void testFilterByContextPartialMatch() throws Exception {
		List<AuditEvent> auditEvents = _auditEventLocalService.getAuditEvents(
			_companyId, null, 0, 0, null, null, null, _eventType, null, null,
			null, null, null, 0, null, "catalog", true, 0, Integer.MAX_VALUE);

		Assert.assertEquals(auditEvents.toString(), 2, auditEvents.size());
	}

	@Test
	public void testGetAuditEventsCountFilterByContext() throws Exception {
		int count = _auditEventLocalService.getAuditEventsCount(
			_companyId, null, 0, 0, null, null, null, _eventType, null, null,
			null, null, null, 0, null, "order-management", true);

		Assert.assertEquals(1, count);
	}

	@Test
	public void testNullContextReturnsAllForEventType() throws Exception {
		List<AuditEvent> auditEvents = _auditEventLocalService.getAuditEvents(
			_companyId, null, 0, 0, null, null, null, _eventType, null, null,
			null, null, null, 0, null, null, true, 0, Integer.MAX_VALUE);

		Assert.assertEquals(auditEvents.toString(), 3, auditEvents.size());
	}

	private void _addAuditEvent(String context) {
		AuditEvent auditEvent = _auditEventLocalService.createAuditEvent(
			RandomTestUtil.randomLong());

		auditEvent.setCompanyId(_companyId);
		auditEvent.setContext(context);
		auditEvent.setEventType(_eventType);
		auditEvent.setCreateDate(new Date());

		_auditEventLocalService.addAuditEvent(auditEvent);
	}

	private AuditEventLocalService _auditEventLocalService;
	private long _companyId;
	private String _eventType;

}