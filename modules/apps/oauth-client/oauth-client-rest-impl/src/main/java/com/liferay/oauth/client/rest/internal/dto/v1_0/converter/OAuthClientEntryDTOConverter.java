/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.oauth.client.rest.internal.dto.v1_0.converter;

import com.liferay.oauth.client.rest.dto.v1_0.OAuthClientEntry;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;

import org.osgi.service.component.annotations.Component;

/**
 * @author Manuele Castro
 */
@Component(
	property = "dto.class.name=com.liferay.oauth.client.persistence.model.OAuthClientEntry",
	service = DTOConverter.class
)
public class OAuthClientEntryDTOConverter
	implements DTOConverter
		<com.liferay.oauth.client.persistence.model.OAuthClientEntry,
		 OAuthClientEntry> {

	@Override
	public String getContentType() {
		return "";
	}

}