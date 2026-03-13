/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

type TOAuthClientEntry = {
    authServerWellKnownURI?: string;
    customClaims?: TCustomClaim[];
    infoJSON?: TInfoJSON;
    matcherField?:
        | 'email'
        | 'screenName';
    oidcUserInfoMapperJSON?: TOIDCUserInfoMapperJSON;
};

type TCustomClaim = {
	expandoColumnName: string;
    index?: number;
	oidcProviderCustomClaim: string;
};

type TInfoJSON = {
    grant_types?: string[];
    application_type?: string;
    client_secret_expires_at?: number;
    scope?: string;
    client_secret?: string;
    client_name?: string;
    token_endpoint_auth_method?: string;
    client_id?: string;
    response_types?: string[];
    id_token_signed_response_alg?: string;
};

type TOIDCUserInfoMapperJSON = {
    address?: TOIDCUserAddress;
    phone?: TOIDCUserPhone;
    contact?: TOIDCUserContact;
    users_roles?: TOIDCUserRoles;
    user?: TOIDCUser;
    users_groups?: TOIDCUserGroups;
};

type TOIDCUser = {
    firstName?: string;
    lastName?: string;
    emailAddress?: string;
    jobTitle?: string;
    languageId?: string;
    middleName?: string;
    screenName?: string;
};

type TOIDCUserAddress = {
    zip?: string;
    country?: string;
    city?: string;
    addressType?: string;
    street?: string;
    region?: string;
};

type TOIDCUserContact = {
    birthdate?: string;
    gender?: string;
};

type TOIDCUserGroups = {
    groups?: string;
};

type TOIDCUserPhone = {
    phoneType?: string;
    phone?: string;
};

type TOIDCUserRoles = {
    roles?: string;
};