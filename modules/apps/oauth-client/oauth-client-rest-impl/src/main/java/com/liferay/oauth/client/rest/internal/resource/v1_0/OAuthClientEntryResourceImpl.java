/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.oauth.client.rest.internal.resource.v1_0;

import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.service.ExpandoColumnLocalService;
import com.liferay.expando.kernel.service.ExpandoTableLocalService;
import com.liferay.exportimport.vulcan.batch.engine.ExportImportVulcanBatchEngineTaskItemDelegate;
import com.liferay.headless.delivery.dto.v1_0.Creator;
import com.liferay.headless.delivery.dto.v1_0.util.CreatorUtil;
import com.liferay.oauth.client.persistence.service.OAuthClientEntryService;
import com.liferay.oauth.client.rest.dto.v1_0.CustomClaim;
import com.liferay.oauth.client.rest.dto.v1_0.CustomField;
import com.liferay.oauth.client.rest.dto.v1_0.OAuthClientEntry;
import com.liferay.oauth.client.rest.resource.v1_0.OAuthClientEntryResource;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.odata.entity.EntityModel;
import com.liferay.portal.vulcan.pagination.Page;

import jakarta.ws.rs.core.MultivaluedHashMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Manuele Castro
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/o-auth-client-entry.properties",
	property = "export.import.vulcan.batch.engine.task.item.delegate=true",
	scope = ServiceScope.PROTOTYPE, service = OAuthClientEntryResource.class
)
public class OAuthClientEntryResourceImpl
	extends BaseOAuthClientEntryResourceImpl
	implements ExportImportVulcanBatchEngineTaskItemDelegate<OAuthClientEntry> {

	@Override
	public void deleteOAuthClientEntryByExternalReferenceCode(
			String oauthClientEntryExternalReferenceCode)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Override
	public EntityModel getEntityModel(Map<String, List<String>> multivaluedMap)
		throws Exception {

		return getEntityModel(
			new MultivaluedHashMap<String, Object>(multivaluedMap));
	}

	@Override
	public ExportImportDescriptor
		<com.liferay.oauth.client.persistence.model.OAuthClientEntry>
			getExportImportDescriptor() {

		return new ExportImportDescriptor<>() {

			@Override
			public String getKey() {
				return OAuthClientEntryResourceImpl.class.getName();
			}

			@Override
			public String getLabelLanguageKey() {
				return "jakarta.portlet.title.com_liferay_oauth_client_admin" +
					"_web_internal_portlet_OAuthClientAdminPortlet";
			}

			@Override
			public Class
				<com.liferay.oauth.client.persistence.model.OAuthClientEntry>
					getModelClass() {

				return com.liferay.oauth.client.persistence.model.
					OAuthClientEntry.class;
			}

			@Override
			public String getPortletId() {
				return "com_liferay_oauth_client_admin_web_internal_portlet" +
					"_OAuthClientAdminPortlet";
			}

			@Override
			public Scope getScope() {
				return Scope.COMPANY;
			}

		};
	}

	@Override
	public Page<OAuthClientEntry> getOAuthClientEntriesPage() throws Exception {
		List<OAuthClientEntry> oAuthClientEntries = new ArrayList<>();

		List<com.liferay.oauth.client.persistence.model.OAuthClientEntry>
			serviceBuilderOAuthClientEntries =
				_oAuthClientEntryService.getCompanyOAuthClientEntries(
					contextCompany.getCompanyId());

		serviceBuilderOAuthClientEntries.forEach(
			serviceBuilderOAuthClientEntry -> oAuthClientEntries.add(
				_toOAuthClientEntry(serviceBuilderOAuthClientEntry)));

		return Page.of(oAuthClientEntries);
	}

	@Override
	public OAuthClientEntry getOAuthClientEntryByExternalReferenceCode(
			String oauthClientEntryExternalReferenceCode)
		throws Exception {

		return new OAuthClientEntry();
	}

	@Override
	public OAuthClientEntry postOAuthClientEntry(
			OAuthClientEntry oAuthClientEntry)
		throws Exception {

		Creator creator = oAuthClientEntry.getCreator();

		com.liferay.oauth.client.persistence.model.OAuthClientEntry
			serviceBuilderOAuthClientEntry =
				_oAuthClientEntryService.addOAuthClientEntry(
					creator.getId(),
					oAuthClientEntry.getAuthRequestParametersJSON(),
					oAuthClientEntry.getAuthServerWellKnownURI(),
					_getCustomClaimsJSON(oAuthClientEntry.getCustomClaims()),
					oAuthClientEntry.getInfoJSON(),
					oAuthClientEntry.getMatcherField(),
					oAuthClientEntry.getMetadataCacheTime(),
					oAuthClientEntry.getOidcUserInfoMapperJSON(),
					oAuthClientEntry.getTokenRequestParametersJSON());

		return _toOAuthClientEntry(serviceBuilderOAuthClientEntry);
	}

	@Override
	public OAuthClientEntry putOAuthClientEntryByExternalReferenceCode(
			String oauthClientEntryExternalReferenceCode,
			OAuthClientEntry oAuthClientEntry)
		throws Exception {

		com.liferay.oauth.client.persistence.model.OAuthClientEntry
			serviceBuilderOAuthClientEntry =
			_oAuthClientEntryService.getOAuthClientEntry(
				contextCompany.getCompanyId(),
				oAuthClientEntry.getAuthServerWellKnownURI(),
				oAuthClientEntry.getClientId());

		oAuthClientEntry.setExternalReferenceCode(
			() -> oauthClientEntryExternalReferenceCode);

		if (serviceBuilderOAuthClientEntry != null) {
			Creator creator = oAuthClientEntry.getCreator();

			serviceBuilderOAuthClientEntry =
				_oAuthClientEntryService.updateOAuthClientEntry(
					creator.getId(),
					oAuthClientEntry.getAuthRequestParametersJSON(),
					oAuthClientEntry.getAuthServerWellKnownURI(),
					_getCustomClaimsJSON(oAuthClientEntry.getCustomClaims()),
					oAuthClientEntry.getInfoJSON(),
					oAuthClientEntry.getMatcherField(),
					oAuthClientEntry.getMetadataCacheTime(),
					oAuthClientEntry.getOidcUserInfoMapperJSON(),
					oAuthClientEntry.getTokenRequestParametersJSON());

			return _toOAuthClientEntry(serviceBuilderOAuthClientEntry);
		}

		return postOAuthClientEntry(oAuthClientEntry);
	}

	private String _getCustomClaimsJSON(CustomClaim[] customClaims) {
		JSONObject customClaimsJSONObject = _jsonFactory.createJSONObject();

		for (CustomClaim customClaim : customClaims) {
			CustomField customField = customClaim.getCustomClaimKey();

			if (customField == null) {
				continue;
			}

			customClaimsJSONObject.put(customField.getName(),
				customClaim::getCustomClaimValue);
		}

		return customClaimsJSONObject.toString();
	}

	private CustomClaim[] _toCustomClaims(
			Long companyId, String customClaimsJSON)
		throws Exception {

		if (Validator.isNull(customClaimsJSON)) {
			return new CustomClaim[0];
		}

		JSONObject customClaimsJSONObject = _jsonFactory.createJSONObject(
			customClaimsJSON);

		List<CustomClaim> customClaimsList = new ArrayList<>();

		Iterator<String> iterator = customClaimsJSONObject.keys();

		while (iterator.hasNext()) {
			String key = iterator.next();

			CustomField customField = _toCustomField(companyId, key);

			if (customField == null) {
				continue;
			}

			customClaimsList.add(
				new CustomClaim() {
					{
						setCustomClaimKey(() -> customField);
						setCustomClaimValue(
							() -> customClaimsJSONObject.getString(key));
					}
				});
		}

		return customClaimsList.toArray(CustomClaim[]::new);
	}

	private CustomField _toCustomField(Long companyId, String key) {
		ExpandoTable expandoTable = _expandoTableLocalService.fetchDefaultTable(
			companyId, User.class.getName());

		if (expandoTable == null) {
			return null;
		}

		ExpandoColumn expandoColumn = _expandoColumnLocalService.fetchColumn(
			expandoTable.getTableId(), key);

		if (expandoColumn == null) {
			return null;
		}

		return new CustomField() {
			{
				setDefaultData(expandoColumn::getDefaultData);
				setFieldType(expandoColumn::getType);
				setFieldTypeSettings(expandoColumn::getTypeSettings);
				setName(() -> key);
			}
		};
	}

	private OAuthClientEntry _toOAuthClientEntry(
		com.liferay.oauth.client.persistence.model.OAuthClientEntry
			serviceBuilderOAuthClientEntry) {

		return new OAuthClientEntry() {
			{
				setAuthRequestParametersJSON(
					serviceBuilderOAuthClientEntry::
						getAuthRequestParametersJSON);
				setAuthServerWellKnownURI(
					serviceBuilderOAuthClientEntry::getAuthServerWellKnownURI);
				setClientId(serviceBuilderOAuthClientEntry::getClientId);
				setCreator(
					() -> CreatorUtil.toCreator(
						null, _portal,
						_userLocalService.fetchUser(
							serviceBuilderOAuthClientEntry.getUserId())));
				setCustomClaims(
					() -> _toCustomClaims(
						serviceBuilderOAuthClientEntry.getCompanyId(),
						serviceBuilderOAuthClientEntry.getCustomClaimsJSON()));
				setDateCreated(serviceBuilderOAuthClientEntry::getCreateDate);
				setDateModified(
					serviceBuilderOAuthClientEntry::getModifiedDate);
				setExternalReferenceCode(
					serviceBuilderOAuthClientEntry::getExternalReferenceCode);
				setInfoJSON(serviceBuilderOAuthClientEntry::getInfoJSON);
				setMatcherField(
					serviceBuilderOAuthClientEntry::getMatcherField);
				setMetadataCacheTime(
					serviceBuilderOAuthClientEntry::getMetadataCacheTime);
				setOidcUserInfoMapperJSON(
					serviceBuilderOAuthClientEntry::getOIDCUserInfoMapperJSON);
				setTokenRequestParametersJSON(
					serviceBuilderOAuthClientEntry::
						getTokenRequestParametersJSON);
			}
		};
	}

	@Reference
	private ExpandoColumnLocalService _expandoColumnLocalService;

	@Reference
	private ExpandoTableLocalService _expandoTableLocalService;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private OAuthClientEntryService _oAuthClientEntryService;

	@Reference
	private Portal _portal;

	@Reference
	private UserLocalService _userLocalService;

}