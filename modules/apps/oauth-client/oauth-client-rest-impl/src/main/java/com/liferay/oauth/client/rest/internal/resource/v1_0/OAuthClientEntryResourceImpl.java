/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.oauth.client.rest.internal.resource.v1_0;

import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.exportimport.vulcan.batch.engine.ExportImportVulcanBatchEngineTaskItemDelegate;
import com.liferay.oauth.client.admin.web.internal.constants.OAuthClientAdminPortletKeys;
import com.liferay.oauth.client.rest.dto.v1_0.OAuthClientEntry;
import com.liferay.oauth.client.rest.resource.v1_0.OAuthClientEntryResource;

import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.vulcan.pagination.Page;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Manuele Castro
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/o-auth-client-entry.properties",
	property = "export.import.vulcan.batch.engine.task.item.delegate=true",
	scope = ServiceScope.PROTOTYPE, service = OAuthClientEntryResource.class
)
public class OAuthClientEntryResourceImpl
	extends BaseOAuthClientEntryResourceImpl implements
	ExportImportVulcanBatchEngineTaskItemDelegate<OAuthClientEntry> {

	@Override
	public Page<OAuthClientEntry> getOAuthClientEntriesPage() throws Exception {
		return Page.of(Collections.emptyList());
	}

	@Override
	public OAuthClientEntry getOAuthClientEntry(
		String oauthClientEntryExternalReferenceCode)
		throws Exception {

		return new OAuthClientEntry();
	}

	@Override
	public OAuthClientEntry postOAuthClientEntry(
		OAuthClientEntry oAuthClientEntry)
		throws Exception {

		return new OAuthClientEntry();
	}

	@Override
	public OAuthClientEntry putOAuthClientEntry(
		String oauthClientEntryExternalReferenceCode,
		OAuthClientEntry oAuthClientEntry)
		throws Exception {

		return new OAuthClientEntry();
	}

	@Override
	public void delete(
		Collection<OAuthClientEntry> oAuthClientEntries,
		Map<String, Serializable> parameters)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Override
	public void update(
		Collection<OAuthClientEntry> oAuthClientEntries,
		Map<String, Serializable> parameters)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Override
	public ExportImportDescriptor<com.liferay.oauth.client.persistence.model.OAuthClientEntry> getExportImportDescriptor() {
		return new ExportImportDescriptor<>() {

			@Override
			public String getKey() {
				return OAuthClientEntryResourceImpl.class.getName();
			}

			@Override
			public String getLabelLanguageKey() {
				return "jakarta.portlet.title.com_liferay_oauth_client_admin_web_internal_portlet_OAuthClientAdminPortlet";
			}

			@Override
			public Class<com.liferay.oauth.client.persistence.model.OAuthClientEntry>
			getModelClass() {

				return com.liferay.oauth.client.persistence.model.OAuthClientEntry.class;
			}

			@Override
			public Map<String, Serializable> getParameters(
				PortletDataContext portletDataContext) {

				return HashMapBuilder.<String, Serializable>put(
					"filter", "modifiable eq true"
				).build();
			}

			@Override
			public String getPortletId() {
				return OAuthClientAdminPortletKeys.OAUTH_CLIENT_ADMIN;
			}

			@Override
			public int getRank() {
				return 99;
			}

			@Override
			public Scope getScope() {
				return Scope.COMPANY;
			}

		};
	}
}