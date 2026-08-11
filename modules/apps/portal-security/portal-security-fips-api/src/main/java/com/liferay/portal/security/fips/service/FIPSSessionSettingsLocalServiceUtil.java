/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service;

import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;

import java.io.Serializable;

import java.util.List;

/**
 * Provides the local service utility for FIPSSessionSettings. This utility wraps
 * <code>com.liferay.portal.security.fips.service.impl.FIPSSessionSettingsLocalServiceImpl</code> and
 * is an access point for service operations in application layer code running
 * on the local server. Methods of this service will not have security checks
 * based on the propagated JAAS credentials because this service can only be
 * accessed from within the same VM.
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettingsLocalService
 * @generated
 */
public class FIPSSessionSettingsLocalServiceUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Add custom service methods to <code>com.liferay.portal.security.fips.service.impl.FIPSSessionSettingsLocalServiceImpl</code> and rerun ServiceBuilder to regenerate this class.
	 */

	/**
	 * Adds the fips session settings to the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect FIPSSessionSettingsLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param fipsSessionSettings the fips session settings
	 * @return the fips session settings that was added
	 */
	public static FIPSSessionSettings addFIPSSessionSettings(
		FIPSSessionSettings fipsSessionSettings) {

		return getService().addFIPSSessionSettings(fipsSessionSettings);
	}

	/**
	 * Creates a new fips session settings with the primary key. Does not add the fips session settings to the database.
	 *
	 * @param fipsSessionSettingsId the primary key for the new fips session settings
	 * @return the new fips session settings
	 */
	public static FIPSSessionSettings createFIPSSessionSettings(
		long fipsSessionSettingsId) {

		return getService().createFIPSSessionSettings(fipsSessionSettingsId);
	}

	/**
	 * @throws PortalException
	 */
	public static PersistedModel createPersistedModel(
			Serializable primaryKeyObj)
		throws PortalException {

		return getService().createPersistedModel(primaryKeyObj);
	}

	/**
	 * Deletes the fips session settings from the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect FIPSSessionSettingsLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param fipsSessionSettings the fips session settings
	 * @return the fips session settings that was removed
	 */
	public static FIPSSessionSettings deleteFIPSSessionSettings(
		FIPSSessionSettings fipsSessionSettings) {

		return getService().deleteFIPSSessionSettings(fipsSessionSettings);
	}

	/**
	 * Deletes the fips session settings with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect FIPSSessionSettingsLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings that was removed
	 * @throws PortalException if a fips session settings with the primary key could not be found
	 */
	public static FIPSSessionSettings deleteFIPSSessionSettings(
			long fipsSessionSettingsId)
		throws PortalException {

		return getService().deleteFIPSSessionSettings(fipsSessionSettingsId);
	}

	/**
	 * @throws PortalException
	 */
	public static PersistedModel deletePersistedModel(
			PersistedModel persistedModel)
		throws PortalException {

		return getService().deletePersistedModel(persistedModel);
	}

	public static <T> T dslQuery(DSLQuery dslQuery) {
		return getService().dslQuery(dslQuery);
	}

	public static int dslQueryCount(DSLQuery dslQuery) {
		return getService().dslQueryCount(dslQuery);
	}

	public static DynamicQuery dynamicQuery() {
		return getService().dynamicQuery();
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	public static <T> List<T> dynamicQuery(DynamicQuery dynamicQuery) {
		return getService().dynamicQuery(dynamicQuery);
	}

	/**
	 * Performs a dynamic query on the database and returns a range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.portal.security.fips.model.impl.FIPSSessionSettingsModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @return the range of matching rows
	 */
	public static <T> List<T> dynamicQuery(
		DynamicQuery dynamicQuery, int start, int end) {

		return getService().dynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * Performs a dynamic query on the database and returns an ordered range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.portal.security.fips.model.impl.FIPSSessionSettingsModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching rows
	 */
	public static <T> List<T> dynamicQuery(
		DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator<T> orderByComparator) {

		return getService().dynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	public static long dynamicQueryCount(DynamicQuery dynamicQuery) {
		return getService().dynamicQueryCount(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	public static long dynamicQueryCount(
		DynamicQuery dynamicQuery,
		com.liferay.portal.kernel.dao.orm.Projection projection) {

		return getService().dynamicQueryCount(dynamicQuery, projection);
	}

	public static FIPSSessionSettings fetchFIPSSessionSettings(
		long fipsSessionSettingsId) {

		return getService().fetchFIPSSessionSettings(fipsSessionSettingsId);
	}

	public static com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery
		getActionableDynamicQuery() {

		return getService().getActionableDynamicQuery();
	}

	/**
	 * Returns the fips session settings with the primary key.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings
	 * @throws PortalException if a fips session settings with the primary key could not be found
	 */
	public static FIPSSessionSettings getFIPSSessionSettings(
			long fipsSessionSettingsId)
		throws PortalException {

		return getService().getFIPSSessionSettings(fipsSessionSettingsId);
	}

	/**
	 * Returns a range of all the fips session settingses.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.portal.security.fips.model.impl.FIPSSessionSettingsModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of fips session settingses
	 * @param end the upper bound of the range of fips session settingses (not inclusive)
	 * @return the range of fips session settingses
	 */
	public static List<FIPSSessionSettings> getFIPSSessionSettingses(
		int start, int end) {

		return getService().getFIPSSessionSettingses(start, end);
	}

	/**
	 * Returns the number of fips session settingses.
	 *
	 * @return the number of fips session settingses
	 */
	public static int getFIPSSessionSettingsesCount() {
		return getService().getFIPSSessionSettingsesCount();
	}

	public static
		com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery
			getIndexableActionableDynamicQuery() {

		return getService().getIndexableActionableDynamicQuery();
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	public static String getOSGiServiceIdentifier() {
		return getService().getOSGiServiceIdentifier();
	}

	/**
	 * @throws PortalException
	 */
	public static PersistedModel getPersistedModel(Serializable primaryKeyObj)
		throws PortalException {

		return getService().getPersistedModel(primaryKeyObj);
	}

	/**
	 * Updates the fips session settings in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect FIPSSessionSettingsLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param fipsSessionSettings the fips session settings
	 * @return the fips session settings that was updated
	 */
	public static FIPSSessionSettings updateFIPSSessionSettings(
		FIPSSessionSettings fipsSessionSettings) {

		return getService().updateFIPSSessionSettings(fipsSessionSettings);
	}

	public static FIPSSessionSettingsLocalService getService() {
		return _serviceSnapshot.get();
	}

	private static final Snapshot<FIPSSessionSettingsLocalService>
		_serviceSnapshot = new Snapshot<>(
			FIPSSessionSettingsLocalServiceUtil.class,
			FIPSSessionSettingsLocalService.class);

}
// LIFERAY-SERVICE-BUILDER-HASH:1367700195