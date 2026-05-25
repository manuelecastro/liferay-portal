/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.audit.rest.internal.resource.v1_0;

import com.liferay.account.constants.AccountConstants;
import com.liferay.account.model.AccountEntry;
import com.liferay.account.service.AccountEntryLocalService;
import com.liferay.headless.delivery.dto.v1_0.util.CreatorUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderByComparatorFactoryUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.audit.rest.dto.v1_0.AuditEvent;
import com.liferay.portal.security.audit.rest.resource.v1_0.AuditEventResource;
import com.liferay.portal.security.audit.storage.model.AuditEventTable;
import com.liferay.portal.security.audit.storage.service.AuditEventService;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Rafael Praxedes
 * @author Manuele Castro
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/audit-event.properties",
	scope = ServiceScope.PROTOTYPE, service = AuditEventResource.class
)
public class AuditEventResourceImpl extends BaseAuditEventResourceImpl {

	@Override
	public Page<AuditEvent> getAuditEventsPage(
			String contextName, Date endDate, String eventType, Date startDate,
			Pagination pagination, Sort[] sorts)
		throws Exception {

		long companyId = contextCompany.getCompanyId();

		List<AccountEntry> accountEntries =
			_accountEntryLocalService.getUserAccountEntries(
				contextUser.getUserId(),
				AccountConstants.PARENT_ACCOUNT_ENTRY_ID_DEFAULT, null,
				new String[] {
					AccountConstants.ACCOUNT_ENTRY_TYPE_BUSINESS,
					AccountConstants.ACCOUNT_ENTRY_TYPE_PERSON
				},
				QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		long[] accountEntryIds = ListUtil.toLongArray(
			accountEntries, AccountEntry::getAccountEntryId);

		return Page.of(
			transform(
				_auditEventService.getAuditEvents(
					companyId, 0, 0, null, startDate, endDate, accountEntryIds,
					null, null, null, null, contextName, eventType, null, 0,
					null, false, pagination.getStartPosition(),
					pagination.getEndPosition(), _toOrderByComparator(sorts)),
				this::_toAuditEvent),
			pagination,
			_auditEventService.getAuditEventsCount(
				companyId, 0, 0, null, startDate, endDate, accountEntryIds,
				null, null, null, null, contextName, eventType, null, 0, null,
				false));
	}

	private AuditEvent _toAuditEvent(
			com.liferay.portal.security.audit.storage.model.AuditEvent
				serviceBuilderAuditEvent)
		throws Exception {

		return new AuditEvent() {
			{
				setAccountId(serviceBuilderAuditEvent::getAccountEntryId);
				setAdditionalInfo(
					() -> _toMap(serviceBuilderAuditEvent.getAdditionalInfo()));
				setClientHost(serviceBuilderAuditEvent::getClientHost);
				setClientIP(serviceBuilderAuditEvent::getClientIP);
				setContextName(serviceBuilderAuditEvent::getContextName);
				setCreator(
					() -> CreatorUtil.toCreator(
						null, _portal,
						_userLocalService.fetchUser(
							serviceBuilderAuditEvent.getUserId())));
				setDateCreated(serviceBuilderAuditEvent::getCreateDate);
				setEntityId(
					() -> GetterUtil.getLong(
						serviceBuilderAuditEvent.getClassPK()));
				setEntityType(serviceBuilderAuditEvent::getClassName);
				setEventType(serviceBuilderAuditEvent::getEventType);
				setId(serviceBuilderAuditEvent::getAuditEventId);
				setMessage(serviceBuilderAuditEvent::getMessage);
				setServerName(serviceBuilderAuditEvent::getServerName);
				setServerPort(serviceBuilderAuditEvent::getServerPort);
				setSessionId(serviceBuilderAuditEvent::getSessionID);
			}
		};
	}

	private Map<String, ?> _toMap(String additionalInfo) throws JSONException {
		if (Validator.isBlank(additionalInfo)) {
			return null;
		}

		JSONObject jsonObject = _jsonFactory.createJSONObject(additionalInfo);

		return jsonObject.toMap();
	}

	private OrderByComparator
		<com.liferay.portal.security.audit.storage.model.AuditEvent>
			_toOrderByComparator(Sort[] sorts) {

		if (ArrayUtil.isEmpty(sorts)) {
			return null;
		}

		List<Object> objects = new ArrayList<>();

		for (Sort sort : sorts) {
			objects.add(sort.getFieldName());
			objects.add(!sort.isReverse());
		}

		return OrderByComparatorFactoryUtil.create(
			AuditEventTable.INSTANCE.getTableName(), objects.toArray());
	}

	@Reference
	private AccountEntryLocalService _accountEntryLocalService;

	@Reference
	private AuditEventService _auditEventService;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Portal _portal;

	@Reference
	private UserLocalService _userLocalService;

}