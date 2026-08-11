/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.persistence;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;

import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The persistence utility for the fips session settings service. This utility wraps <code>com.liferay.portal.security.fips.service.persistence.impl.FIPSSessionSettingsPersistenceImpl</code> and provides direct access to the database for CRUD operations. This utility should only be used by the service layer, as it must operate within a transaction. Never access this utility in a JSP, controller, model, or other front-end class.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettingsPersistence
 * @generated
 */
public class FIPSSessionSettingsUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#cacheResult(List)
	 */
	public static void cacheResult(
		List<FIPSSessionSettings> fipsSessionSettingses) {

		getPersistence().cacheResult(fipsSessionSettingses);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#cacheResult(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static void cacheResult(FIPSSessionSettings fipsSessionSettings) {
		getPersistence().cacheResult(fipsSessionSettings);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#clearCache()
	 */
	public static void clearCache() {
		getPersistence().clearCache();
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#clearCache(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static void clearCache(FIPSSessionSettings fipsSessionSettings) {
		getPersistence().clearCache(fipsSessionSettings);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#countWithDynamicQuery(DynamicQuery)
	 */
	public static long countWithDynamicQuery(DynamicQuery dynamicQuery) {
		return getPersistence().countWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#fetchByPrimaryKeys(Set)
	 */
	public static Map<Serializable, FIPSSessionSettings> fetchByPrimaryKeys(
		Set<Serializable> primaryKeys) {

		return getPersistence().fetchByPrimaryKeys(primaryKeys);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery)
	 */
	public static List<FIPSSessionSettings> findWithDynamicQuery(
		DynamicQuery dynamicQuery) {

		return getPersistence().findWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery, int, int)
	 */
	public static List<FIPSSessionSettings> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end) {

		return getPersistence().findWithDynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery, int, int, OrderByComparator)
	 */
	public static List<FIPSSessionSettings> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator<FIPSSessionSettings> orderByComparator) {

		return getPersistence().findWithDynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#update(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static FIPSSessionSettings update(
		FIPSSessionSettings fipsSessionSettings) {

		return getPersistence().update(fipsSessionSettings);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#update(com.liferay.portal.kernel.model.BaseModel, ServiceContext)
	 */
	public static FIPSSessionSettings update(
		FIPSSessionSettings fipsSessionSettings,
		ServiceContext serviceContext) {

		return getPersistence().update(fipsSessionSettings, serviceContext);
	}

	/**
	 * Returns the fips session settings where companyId = &#63; or throws a <code>NoSuchSessionSettingsException</code> if it could not be found.
	 *
	 * @param companyId the company ID
	 * @return the matching fips session settings
	 * @throws NoSuchSessionSettingsException if a matching fips session settings could not be found
	 */
	public static FIPSSessionSettings findByCompanyId(long companyId)
		throws com.liferay.portal.security.fips.exception.
			NoSuchSessionSettingsException {

		return getPersistence().findByCompanyId(companyId);
	}

	/**
	 * Returns the fips session settings where companyId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param companyId the company ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching fips session settings, or <code>null</code> if a matching fips session settings could not be found
	 */
	public static FIPSSessionSettings fetchByCompanyId(
		long companyId, boolean useFinderCache) {

		return getPersistence().fetchByCompanyId(companyId, useFinderCache);
	}

	/**
	 * Removes the fips session settings where companyId = &#63; from the database.
	 *
	 * @param companyId the company ID
	 * @return the fips session settings that was removed
	 */
	public static FIPSSessionSettings removeByCompanyId(long companyId)
		throws com.liferay.portal.security.fips.exception.
			NoSuchSessionSettingsException {

		return getPersistence().removeByCompanyId(companyId);
	}

	/**
	 * Returns the number of fips session settingses where companyId = &#63;.
	 *
	 * @param companyId the company ID
	 * @return the number of matching fips session settingses
	 */
	public static int countByCompanyId(long companyId) {
		return getPersistence().countByCompanyId(companyId);
	}

	/**
	 * Creates a new fips session settings with the primary key. Does not add the fips session settings to the database.
	 *
	 * @param fipsSessionSettingsId the primary key for the new fips session settings
	 * @return the new fips session settings
	 */
	public static FIPSSessionSettings create(long fipsSessionSettingsId) {
		return getPersistence().create(fipsSessionSettingsId);
	}

	/**
	 * Removes the fips session settings with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings that was removed
	 * @throws NoSuchSessionSettingsException if a fips session settings with the primary key could not be found
	 */
	public static FIPSSessionSettings remove(long fipsSessionSettingsId)
		throws com.liferay.portal.security.fips.exception.
			NoSuchSessionSettingsException {

		return getPersistence().remove(fipsSessionSettingsId);
	}

	public static FIPSSessionSettings updateImpl(
		FIPSSessionSettings fipsSessionSettings) {

		return getPersistence().updateImpl(fipsSessionSettings);
	}

	/**
	 * Returns the fips session settings with the primary key or throws a <code>NoSuchSessionSettingsException</code> if it could not be found.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings
	 * @throws NoSuchSessionSettingsException if a fips session settings with the primary key could not be found
	 */
	public static FIPSSessionSettings findByPrimaryKey(
			long fipsSessionSettingsId)
		throws com.liferay.portal.security.fips.exception.
			NoSuchSessionSettingsException {

		return getPersistence().findByPrimaryKey(fipsSessionSettingsId);
	}

	/**
	 * Returns the fips session settings with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings, or <code>null</code> if a fips session settings with the primary key could not be found
	 */
	public static FIPSSessionSettings fetchByPrimaryKey(
		long fipsSessionSettingsId) {

		return getPersistence().fetchByPrimaryKey(fipsSessionSettingsId);
	}

	/**
	 * Returns the fips session settings where companyId = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param companyId the company ID
	 * @return the matching fips session settings, or <code>null</code> if a matching fips session settings could not be found
	 */
	public static FIPSSessionSettings fetchByCompanyId(long companyId) {
		return getPersistence().fetchByCompanyId(companyId);
	}

	public static FIPSSessionSettingsPersistence getPersistence() {
		return _persistence;
	}

	public static void setPersistence(
		FIPSSessionSettingsPersistence persistence) {

		_persistence = persistence;
	}

	private static volatile FIPSSessionSettingsPersistence _persistence;

}
// LIFERAY-SERVICE-BUILDER-HASH:451179483