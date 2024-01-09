/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.service.impl;

import com.liferay.portal.kernel.model.RememberMeToken;
import com.liferay.portal.kernel.service.persistence.RememberMeTokenPersistence;
import com.liferay.portal.kernel.service.persistence.RememberMeTokenUtil;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.service.base.RememberMeTokenLocalServiceBaseImpl;

import java.util.Date;
import java.util.List;

/**
 * @author Brian Wing Shun Chan
 */
public class RememberMeTokenLocalServiceImpl
	extends RememberMeTokenLocalServiceBaseImpl {
	@Override
	public RememberMeToken addRememberMeToken(
			long companyId, long userId, Date expirationDate) {

		long rememberMeTokenId = counterLocalService.increment();

		RememberMeToken rememberMeToken = rememberMeTokenPersistence.create(rememberMeTokenId);

		rememberMeToken.setCompanyId(companyId);
		rememberMeToken.setCreateDate(new Date());
		rememberMeToken.setUserId(userId);
		rememberMeToken.setAccessToken(PortalUUIDUtil.generate());
		rememberMeToken.setExpirationDate(expirationDate);
		rememberMeToken.setUserName("test");

		return rememberMeTokenPersistence.update(rememberMeToken);
	}

	public List<RememberMeToken> getUserRememberMeTokens(long companyId, long userId) {
		return RememberMeTokenUtil.findByC_U(companyId, userId);
	}
}