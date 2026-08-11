/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service;

import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.service.persistence.BasePersistence;

/**
 * Provides a wrapper for {@link FIPSSessionSettingsLocalService}.
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettingsLocalService
 * @generated
 */
public class FIPSSessionSettingsLocalServiceWrapper
	implements FIPSSessionSettingsLocalService,
			   ServiceWrapper<FIPSSessionSettingsLocalService> {

	public FIPSSessionSettingsLocalServiceWrapper() {
		this(null);
	}

	public FIPSSessionSettingsLocalServiceWrapper(
		FIPSSessionSettingsLocalService fipsSessionSettingsLocalService) {

		_fipsSessionSettingsLocalService = fipsSessionSettingsLocalService;
	}

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
	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
		addFIPSSessionSettings(
			com.liferay.portal.security.fips.model.FIPSSessionSettings
				fipsSessionSettings) {

		return _fipsSessionSettingsLocalService.addFIPSSessionSettings(
			fipsSessionSettings);
	}

	/**
	 * Creates a new fips session settings with the primary key. Does not add the fips session settings to the database.
	 *
	 * @param fipsSessionSettingsId the primary key for the new fips session settings
	 * @return the new fips session settings
	 */
	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
		createFIPSSessionSettings(long fipsSessionSettingsId) {

		return _fipsSessionSettingsLocalService.createFIPSSessionSettings(
			fipsSessionSettingsId);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel createPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _fipsSessionSettingsLocalService.createPersistedModel(
			primaryKeyObj);
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
	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
		deleteFIPSSessionSettings(
			com.liferay.portal.security.fips.model.FIPSSessionSettings
				fipsSessionSettings) {

		return _fipsSessionSettingsLocalService.deleteFIPSSessionSettings(
			fipsSessionSettings);
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
	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
			deleteFIPSSessionSettings(long fipsSessionSettingsId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _fipsSessionSettingsLocalService.deleteFIPSSessionSettings(
			fipsSessionSettingsId);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel deletePersistedModel(
			com.liferay.portal.kernel.model.PersistedModel persistedModel)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _fipsSessionSettingsLocalService.deletePersistedModel(
			persistedModel);
	}

	@Override
	public <T> T dslQuery(com.liferay.petra.sql.dsl.query.DSLQuery dslQuery) {
		return _fipsSessionSettingsLocalService.dslQuery(dslQuery);
	}

	@Override
	public int dslQueryCount(
		com.liferay.petra.sql.dsl.query.DSLQuery dslQuery) {

		return _fipsSessionSettingsLocalService.dslQueryCount(dslQuery);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery() {
		return _fipsSessionSettingsLocalService.dynamicQuery();
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _fipsSessionSettingsLocalService.dynamicQuery(dynamicQuery);
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
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end) {

		return _fipsSessionSettingsLocalService.dynamicQuery(
			dynamicQuery, start, end);
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
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end,
		com.liferay.portal.kernel.util.OrderByComparator<T> orderByComparator) {

		return _fipsSessionSettingsLocalService.dynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _fipsSessionSettingsLocalService.dynamicQueryCount(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery,
		com.liferay.portal.kernel.dao.orm.Projection projection) {

		return _fipsSessionSettingsLocalService.dynamicQueryCount(
			dynamicQuery, projection);
	}

	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
		fetchFIPSSessionSettings(long fipsSessionSettingsId) {

		return _fipsSessionSettingsLocalService.fetchFIPSSessionSettings(
			fipsSessionSettingsId);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery
		getActionableDynamicQuery() {

		return _fipsSessionSettingsLocalService.getActionableDynamicQuery();
	}

	/**
	 * Returns the settings of the company, or a transient instance carrying the
	 * deployment defaults when the company has never saved any. Callers are
	 * therefore free of null checks.
	 */
	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
		getCompanyFIPSSessionSettings(long companyId) {

		return _fipsSessionSettingsLocalService.getCompanyFIPSSessionSettings(
			companyId);
	}

	/**
	 * Returns the fips session settings with the primary key.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings
	 * @throws PortalException if a fips session settings with the primary key could not be found
	 */
	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
			getFIPSSessionSettings(long fipsSessionSettingsId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _fipsSessionSettingsLocalService.getFIPSSessionSettings(
			fipsSessionSettingsId);
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
	@Override
	public java.util.List
		<com.liferay.portal.security.fips.model.FIPSSessionSettings>
			getFIPSSessionSettingses(int start, int end) {

		return _fipsSessionSettingsLocalService.getFIPSSessionSettingses(
			start, end);
	}

	/**
	 * Returns the number of fips session settingses.
	 *
	 * @return the number of fips session settingses
	 */
	@Override
	public int getFIPSSessionSettingsesCount() {
		return _fipsSessionSettingsLocalService.getFIPSSessionSettingsesCount();
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery
		getIndexableActionableDynamicQuery() {

		return _fipsSessionSettingsLocalService.
			getIndexableActionableDynamicQuery();
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return _fipsSessionSettingsLocalService.getOSGiServiceIdentifier();
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel getPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _fipsSessionSettingsLocalService.getPersistedModel(
			primaryKeyObj);
	}

	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
			updateCompanyFIPSSessionSettings(
				long userId, long companyId, int idleTimeoutMinutes,
				int absoluteLifetimeMinutes)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _fipsSessionSettingsLocalService.
			updateCompanyFIPSSessionSettings(
				userId, companyId, idleTimeoutMinutes, absoluteLifetimeMinutes);
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
	@Override
	public com.liferay.portal.security.fips.model.FIPSSessionSettings
		updateFIPSSessionSettings(
			com.liferay.portal.security.fips.model.FIPSSessionSettings
				fipsSessionSettings) {

		return _fipsSessionSettingsLocalService.updateFIPSSessionSettings(
			fipsSessionSettings);
	}

	@Override
	public BasePersistence<?> getBasePersistence() {
		return _fipsSessionSettingsLocalService.getBasePersistence();
	}

	@Override
	public FIPSSessionSettingsLocalService getWrappedService() {
		return _fipsSessionSettingsLocalService;
	}

	@Override
	public void setWrappedService(
		FIPSSessionSettingsLocalService fipsSessionSettingsLocalService) {

		_fipsSessionSettingsLocalService = fipsSessionSettingsLocalService;
	}

	private FIPSSessionSettingsLocalService _fipsSessionSettingsLocalService;

}
// LIFERAY-SERVICE-BUILDER-HASH:-1722402273