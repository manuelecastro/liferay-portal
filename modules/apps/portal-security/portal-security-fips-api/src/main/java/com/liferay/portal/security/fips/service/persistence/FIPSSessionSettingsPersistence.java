/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.persistence;

import com.liferay.portal.kernel.service.persistence.BasePersistence;
import com.liferay.portal.security.fips.exception.NoSuchSessionSettingsException;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The persistence interface for the fips session settings service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettingsUtil
 * @generated
 */
@ProviderType
public interface FIPSSessionSettingsPersistence
	extends BasePersistence<FIPSSessionSettings> {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this interface directly. Always use {@link FIPSSessionSettingsUtil} to access the fips session settings persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this interface.
	 */

	/**
	 * Returns the fips session settings where companyId = &#63; or throws a <code>NoSuchSessionSettingsException</code> if it could not be found.
	 *
	 * @param companyId the company ID
	 * @return the matching fips session settings
	 * @throws NoSuchSessionSettingsException if a matching fips session settings could not be found
	 */
	public FIPSSessionSettings findByCompanyId(long companyId)
		throws NoSuchSessionSettingsException;

	/**
	 * Returns the fips session settings where companyId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param companyId the company ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching fips session settings, or <code>null</code> if a matching fips session settings could not be found
	 */
	public FIPSSessionSettings fetchByCompanyId(
		long companyId, boolean useFinderCache);

	/**
	 * Removes the fips session settings where companyId = &#63; from the database.
	 *
	 * @param companyId the company ID
	 * @return the fips session settings that was removed
	 */
	public FIPSSessionSettings removeByCompanyId(long companyId)
		throws NoSuchSessionSettingsException;

	/**
	 * Returns the number of fips session settingses where companyId = &#63;.
	 *
	 * @param companyId the company ID
	 * @return the number of matching fips session settingses
	 */
	public int countByCompanyId(long companyId);

	/**
	 * Creates a new fips session settings with the primary key. Does not add the fips session settings to the database.
	 *
	 * @param fipsSessionSettingsId the primary key for the new fips session settings
	 * @return the new fips session settings
	 */
	public FIPSSessionSettings create(long fipsSessionSettingsId);

	/**
	 * Removes the fips session settings with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings that was removed
	 * @throws NoSuchSessionSettingsException if a fips session settings with the primary key could not be found
	 */
	public FIPSSessionSettings remove(long fipsSessionSettingsId)
		throws NoSuchSessionSettingsException;

	public FIPSSessionSettings updateImpl(
		FIPSSessionSettings fipsSessionSettings);

	/**
	 * Returns the fips session settings with the primary key or throws a <code>NoSuchSessionSettingsException</code> if it could not be found.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings
	 * @throws NoSuchSessionSettingsException if a fips session settings with the primary key could not be found
	 */
	public FIPSSessionSettings findByPrimaryKey(long fipsSessionSettingsId)
		throws NoSuchSessionSettingsException;

	/**
	 * Returns the fips session settings with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings, or <code>null</code> if a fips session settings with the primary key could not be found
	 */
	public FIPSSessionSettings fetchByPrimaryKey(long fipsSessionSettingsId);

	/**
	 * Returns the fips session settings where companyId = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param companyId the company ID
	 * @return the matching fips session settings, or <code>null</code> if a matching fips session settings could not be found
	 */
	public default FIPSSessionSettings fetchByCompanyId(long companyId) {
		return fetchByCompanyId(companyId, true);
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:-1647762741