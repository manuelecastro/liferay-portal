/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.internal.upgrade.v10_23_0;

import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.service.ResourceActionLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Manuele Castro
 */
public class ObjectDefinitionClassNameResourcePermissionUpgradeProcess
	extends UpgradeProcess {

	public ObjectDefinitionClassNameResourcePermissionUpgradeProcess(
		ObjectDefinitionLocalService objectDefinitionLocalService,
		ResourceActionLocalService resourceActionLocalService,
		ResourcePermissionLocalService resourcePermissionLocalService) {

		_objectDefinitionLocalService = objectDefinitionLocalService;
		_resourceActionLocalService = resourceActionLocalService;
		_resourcePermissionLocalService = resourcePermissionLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select objectDefinitionId, name from ObjectField where " +
					"businessType = 'Attachment'");
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				ObjectDefinition objectDefinition =
					_objectDefinitionLocalService.fetchObjectDefinition(
						resultSet.getLong(1));

				if ((objectDefinition != null) &&
					objectDefinition.isApproved()) {

					String objectDefinitionClassName =
						objectDefinition.getClassName();

					String objectFieldActionId =
						ObjectFieldConstants.
							ATTACHMENT_FIELD_DOWNLOAD_ACTION_ID_PREFIX +
								resultSet.getString(2);

					ResourceAction resourceAction =
						_resourceActionLocalService.fetchResourceAction(
							objectDefinitionClassName, objectFieldActionId);

					if (resourceAction == null) {
						resourceAction = _addResourceAction(
							objectFieldActionId, objectDefinitionClassName);
					}

					_updateObjectDefinitionClassNameResourcePermissions(
						objectDefinitionClassName, resourceAction);
				}
			}
		}
	}

	private ResourceAction _addResourceAction(String actionId, String name)
		throws Exception {

		long bitwiseValue = _getNextBitwiseValue(name);

		if (bitwiseValue <= 1) {
			return null;
		}

		long resourceActionId = increment(ResourceAction.class.getName());

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"insert into ResourceAction (mvccVersion, ",
					"resourceActionId, name, actionId, bitwiseValue) values ",
					"(?, ?, ?, ?, ?)"))) {

			preparedStatement.setLong(1, 0);
			preparedStatement.setLong(2, resourceActionId);
			preparedStatement.setString(3, name);
			preparedStatement.setString(4, actionId);
			preparedStatement.setLong(5, bitwiseValue);

			preparedStatement.executeUpdate();

			return _resourceActionLocalService.getResourceAction(
				name, actionId);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to add ResourceAction for " + actionId, exception);
			}
		}

		return null;
	}

	private long _getNextBitwiseValue(String resourceActionName)
		throws Exception {

		long nextBitwiseValue = 1;

		long combinedBitwiseValues = 0;

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select bitwiseValue from ResourceAction where name = " +
					resourceActionName);
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				combinedBitwiseValues |= resultSet.getLong(1);
			}
		}

		while ((combinedBitwiseValues & nextBitwiseValue) != 0) {
			nextBitwiseValue <<= 1;
		}

		return nextBitwiseValue;
	}

	private void _updateObjectDefinitionClassNameResourcePermissions(
		String name, ResourceAction resourceAction) {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select resourcePermissionId, actionIds from ",
					"ResourcePermission where name = ", name, " and scope = ",
					ResourceConstants.SCOPE_INDIVIDUAL));
			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				ResourcePermission resourcePermission =
					_resourcePermissionLocalService.fetchResourcePermission(
						resultSet.getLong(1));

				String resourceActionId = resourceAction.getActionId();

				if ((resourcePermission != null) &&
					((resultSet.getLong(2) % 2) != 0) &&
					!resourcePermission.hasActionId(resourceActionId)) {

					resourcePermission.addResourceAction(resourceActionId);

					_resourcePermissionLocalService.updateResourcePermission(
						resourcePermission);
				}
			}
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to update ResourcePermission for " + name,
					exception);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ObjectDefinitionClassNameResourcePermissionUpgradeProcess.class);

	private final ObjectDefinitionLocalService _objectDefinitionLocalService;
	private final ResourceActionLocalService _resourceActionLocalService;
	private final ResourcePermissionLocalService
		_resourcePermissionLocalService;

}