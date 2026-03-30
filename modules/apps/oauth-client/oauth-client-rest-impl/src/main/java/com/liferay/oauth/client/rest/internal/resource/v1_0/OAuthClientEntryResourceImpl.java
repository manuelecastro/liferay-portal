/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.oauth.client.rest.internal.resource.v1_0;

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.util.ExpandoBridgeFactoryUtil;
import com.liferay.exportimport.vulcan.batch.engine.ExportImportVulcanBatchEngineTaskItemDelegate;
import com.liferay.headless.delivery.dto.v1_0.util.CreatorUtil;
import com.liferay.oauth.client.constants.OAuthClientAdminPortletKeys;
import com.liferay.oauth.client.persistence.exception.NoSuchOAuthClientEntryException;
import com.liferay.oauth.client.persistence.service.OAuthClientASLocalMetadataLocalService;
import com.liferay.oauth.client.persistence.service.OAuthClientASLocalMetadataService;
import com.liferay.oauth.client.persistence.service.OAuthClientEntryService;
import com.liferay.oauth.client.rest.dto.v1_0.CustomClaim;
import com.liferay.oauth.client.rest.dto.v1_0.CustomField;
import com.liferay.oauth.client.rest.dto.v1_0.OAuthClientASLocalMetadata;
import com.liferay.oauth.client.rest.dto.v1_0.OAuthClientEntry;
import com.liferay.oauth.client.rest.resource.v1_0.OAuthClientEntryResource;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.GuestOrUserUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.vulcan.pagination.Page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

		com.liferay.oauth.client.persistence.model.OAuthClientEntry
			oAuthClientEntry =
				_oAuthClientEntryService.
					fetchOAuthClientEntryByExternalReferenceCode(
						oauthClientEntryExternalReferenceCode,
						contextCompany.getCompanyId());

		if (oAuthClientEntry == null) {
			throw new NoSuchOAuthClientEntryException(
				"Unable to find OAuthClientEntry with external reference " +
					"code " + oauthClientEntryExternalReferenceCode);
		}

		_oAuthClientEntryService.deleteOAuthClientEntry(
			oAuthClientEntry.getOAuthClientEntryId());
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
				return "oauth-client-entries";
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
				return OAuthClientAdminPortletKeys.OAUTH_CLIENT_ADMIN;
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

		com.liferay.oauth.client.persistence.model.OAuthClientEntry
			serviceBuilderOAuthClientEntry =
				_oAuthClientEntryService.
					fetchOAuthClientEntryByExternalReferenceCode(
						oauthClientEntryExternalReferenceCode,
						contextCompany.getCompanyId());

		return _toOAuthClientEntry(serviceBuilderOAuthClientEntry);
	}

	@Override
	public OAuthClientEntry postOAuthClientEntry(
			OAuthClientEntry oAuthClientEntry)
		throws Exception {

		String authServerWellKnownURI =
			oAuthClientEntry.getAuthServerWellKnownURI();

		if (authServerWellKnownURI.endsWith("local")) {
			OAuthClientASLocalMetadata oAuthClientASLocalMetadata =
				oAuthClientEntry.getOAuthClientASLocalMetadata();

			if (oAuthClientASLocalMetadata != null) {
				com.liferay.oauth.client.persistence.model.
					OAuthClientASLocalMetadata
						serviceBuilderOAuthClientASLocalMetadata =
							_oAuthClientASLocalMetadataService.
								fetchOAuthClientASLocalMetadata(
									contextCompany.getCompanyId(),
									oAuthClientASLocalMetadata.getIssuer());

				if (serviceBuilderOAuthClientASLocalMetadata == null) {
					_oAuthClientASLocalMetadataService.
						addOAuthClientASLocalMetadata(
							oAuthClientASLocalMetadata.getOAuthASMetadataJSON(),
							"openid-configuration");
				}
			}
		}

		com.liferay.oauth.client.persistence.model.OAuthClientEntry
			serviceBuilderOAuthClientEntry =
				_oAuthClientEntryService.addOAuthClientEntry(
					oAuthClientEntry.getExternalReferenceCode(),
					GuestOrUserUtil.getUserId(),
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
				_oAuthClientEntryService.
					fetchOAuthClientEntryByExternalReferenceCode(
						oauthClientEntryExternalReferenceCode,
						contextCompany.getCompanyId());

		oAuthClientEntry.setExternalReferenceCode(
			() -> oauthClientEntryExternalReferenceCode);

		if (serviceBuilderOAuthClientEntry != null) {
			serviceBuilderOAuthClientEntry =
				_oAuthClientEntryService.updateOAuthClientEntry(
					serviceBuilderOAuthClientEntry.getOAuthClientEntryId(),
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

			ExpandoBridge expandoBridge =
				ExpandoBridgeFactoryUtil.getExpandoBridge(
					contextCompany.getCompanyId(), User.class.getName());

			if (!expandoBridge.hasAttribute(customField.getName())) {
				continue;
			}

			customClaimsJSONObject.put(
				customField.getName(), customClaim::getCustomClaimValue);
		}

		return customClaimsJSONObject.toString();
	}

	private CustomClaim[] _toCustomClaims(String customClaimsJSON)
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

			CustomField customField = _toCustomField(key);

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

	private CustomField _toCustomField(String key) {
		ExpandoBridge expandoBridge = ExpandoBridgeFactoryUtil.getExpandoBridge(
			contextCompany.getCompanyId(), User.class.getName());

		if (!expandoBridge.hasAttribute(key)) {
			return null;
		}

		return new CustomField() {
			{
				setDefaultData(
					() -> String.valueOf(
						expandoBridge.getAttributeDefault(key)));
				setFieldType(() -> expandoBridge.getAttributeType(key));
				setFieldTypeSettings(
					() -> String.valueOf(
						expandoBridge.getAttributeProperties(key)));
				setName(() -> key);
			}
		};
	}

	private OAuthClientASLocalMetadata _toOAuthClientASLocalMetadata(
		String wellKnownURI) {

		if (!wellKnownURI.endsWith("local")) {
			return null;
		}

		com.liferay.oauth.client.persistence.model.OAuthClientASLocalMetadata
			serviceBuilderOAuthClientASLocalMetadata =
				_oAuthClientASLocalMetadataLocalService.
					fetchOAuthClientASLocalMetadata(
						contextCompany.getCompanyId(), wellKnownURI);

		if (serviceBuilderOAuthClientASLocalMetadata == null) {
			return null;
		}

		return new OAuthClientASLocalMetadata() {
			{
				setCreator(
					() -> CreatorUtil.toCreator(
						null, _portal,
						_userLocalService.fetchUser(
							serviceBuilderOAuthClientASLocalMetadata.
								getUserId())));
				setDateCreated(
					serviceBuilderOAuthClientASLocalMetadata::getCreateDate);
				setDateModified(
					serviceBuilderOAuthClientASLocalMetadata::getModifiedDate);
				setIssuer(serviceBuilderOAuthClientASLocalMetadata::getIssuer);
				setLocalWellKnownEnabled(
					serviceBuilderOAuthClientASLocalMetadata::
						getLocalWellKnownEnabled);
				setLocalWellKnownURI(
					serviceBuilderOAuthClientASLocalMetadata::
						getLocalWellKnownURI);
				setMetadataJSON(
					serviceBuilderOAuthClientASLocalMetadata::getMetadataJSON);
				setOAuthASLocalWellKnownURI(
					serviceBuilderOAuthClientASLocalMetadata::
						getOAuthASLocalWellKnownURI);
				setOAuthASMetadataJSON(
					serviceBuilderOAuthClientASLocalMetadata::
						getOAuthASMetadataJSON);
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
				setOAuthClientASLocalMetadata(
					() -> _toOAuthClientASLocalMetadata(
						serviceBuilderOAuthClientEntry.
							getAuthServerWellKnownURI()));
				setOidcUserInfoMapperJSON(
					serviceBuilderOAuthClientEntry::getOIDCUserInfoMapperJSON);
				setTokenRequestParametersJSON(
					serviceBuilderOAuthClientEntry::
						getTokenRequestParametersJSON);
			}
		};
	}

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private OAuthClientASLocalMetadataLocalService
		_oAuthClientASLocalMetadataLocalService;

	@Reference
	private OAuthClientASLocalMetadataService
		_oAuthClientASLocalMetadataService;

	@Reference
	private OAuthClientEntryService _oAuthClientEntryService;

	@Reference
	private Portal _portal;

	@Reference
	private UserLocalService _userLocalService;

}