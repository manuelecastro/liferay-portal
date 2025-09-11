/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.internal.servlet.filter;

import com.liferay.object.exception.NoSuchObjectEntryException;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.util.HttpServletRequestThreadLocal;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.login.AuthLoginGroupSettingsUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermissionRegistryUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.BaseFilter;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Pedro Tavares
 */
@Component(
	property = {
		"servlet-context-name=", "servlet-filter-name=Object Context Filter",
		"url-pattern=/documents/*", "url-pattern=/image/*", "url-pattern=/o/c/*"
	},
	service = Filter.class
)
public class ObjectContextFilter extends BaseFilter {

	@Override
	protected Log getLog() {
		return _log;
	}

	@Override
	protected void processFilter(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws Exception {

		if (Validator.isNotNull(
				ParamUtil.getString(
					httpServletRequest, "objectFieldActionId"))) {

			_checkObjectEntryModelResourcePermission(
				httpServletRequest, httpServletResponse);
		}

		HttpServletRequestThreadLocal.setHttpServletRequest(httpServletRequest);

		try {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		}
		finally {
			HttpServletRequestThreadLocal.setHttpServletRequest(null);
		}
	}

	private void _checkObjectEntryModelResourcePermission(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws Exception {

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.
				fetchObjectDefinitionByExternalReferenceCode(
					ParamUtil.getString(
						httpServletRequest,
						"objectDefinitionExternalReferenceCode"),
					PortalUtil.getCompanyId(httpServletRequest));

		if (objectDefinition == null) {
			return;
		}

		ObjectEntry objectEntry = _objectEntryLocalService.fetchObjectEntry(
			ParamUtil.getString(
				httpServletRequest, "objectEntryExternalReferenceCode"),
			PortalUtil.getScopeGroupId(httpServletRequest),
			objectDefinition.getObjectDefinitionId());

		if (objectEntry == null) {
			return;
		}

		PermissionChecker permissionChecker = _getPermissionChecker(
			httpServletRequest);

		ModelResourcePermission<?> objectEntryModelResourcePermission =
			ModelResourcePermissionRegistryUtil.getModelResourcePermission(
				objectDefinition.getClassName());

		try {
			objectEntryModelResourcePermission.check(
				permissionChecker, objectEntry.getObjectEntryId(),
				ActionKeys.VIEW);

			String objectFieldActionId = ParamUtil.getString(
				httpServletRequest, "objectFieldActionId");

			if (ParamUtil.getBoolean(httpServletRequest, "download") &&
				Validator.isNotNull(objectFieldActionId)) {

				objectEntryModelResourcePermission.check(
					permissionChecker, objectEntry.getObjectEntryId(),
					objectFieldActionId);
			}
		}
		catch (PortalException portalException) {
			User user = permissionChecker.getUser();

			if (user.isGuestUser() &&
				!AuthLoginGroupSettingsUtil.isPromptEnabled(
					objectEntry.getGroupId())) {

				PortalUtil.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					new NoSuchObjectEntryException(portalException),
					httpServletRequest, httpServletResponse);
			}

			PortalUtil.sendError(
				portalException, httpServletRequest, httpServletResponse);
		}
	}

	private PermissionChecker _getPermissionChecker(
			HttpServletRequest httpServletRequest)
		throws Exception {

		User user = PortalUtil.getUser(httpServletRequest);

		if (user == null) {
			user = _userLocalService.getGuestUser(
				PortalUtil.getCompanyId(httpServletRequest));
		}

		return PermissionThreadLocal.getPermissionChecker(
			user, !user.isGuestUser());
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ObjectContextFilter.class);

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

	@Reference
	private UserLocalService _userLocalService;

}