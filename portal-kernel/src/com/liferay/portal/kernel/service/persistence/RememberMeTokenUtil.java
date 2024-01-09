/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.service.persistence;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.model.RememberMeToken;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.OrderByComparator;

import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The persistence utility for the remember me token service. This utility wraps <code>com.liferay.portal.service.persistence.impl.RememberMeTokenPersistenceImpl</code> and provides direct access to the database for CRUD operations. This utility should only be used by the service layer, as it must operate within a transaction. Never access this utility in a JSP, controller, model, or other front-end class.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see RememberMeTokenPersistence
 * @generated
 */
public class RememberMeTokenUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */

	/**
	 * @see BasePersistence#clearCache()
	 */
	public static void clearCache() {
		getPersistence().clearCache();
	}

	/**
	 * @see BasePersistence#clearCache(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static void clearCache(RememberMeToken rememberMeToken) {
		getPersistence().clearCache(rememberMeToken);
	}

	/**
	 * @see BasePersistence#countWithDynamicQuery(DynamicQuery)
	 */
	public static long countWithDynamicQuery(DynamicQuery dynamicQuery) {
		return getPersistence().countWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see BasePersistence#fetchByPrimaryKeys(Set)
	 */
	public static Map<Serializable, RememberMeToken> fetchByPrimaryKeys(
		Set<Serializable> primaryKeys) {

		return getPersistence().fetchByPrimaryKeys(primaryKeys);
	}

	/**
	 * @see BasePersistence#findWithDynamicQuery(DynamicQuery)
	 */
	public static List<RememberMeToken> findWithDynamicQuery(
		DynamicQuery dynamicQuery) {

		return getPersistence().findWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see BasePersistence#findWithDynamicQuery(DynamicQuery, int, int)
	 */
	public static List<RememberMeToken> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end) {

		return getPersistence().findWithDynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * @see BasePersistence#findWithDynamicQuery(DynamicQuery, int, int, OrderByComparator)
	 */
	public static List<RememberMeToken> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator) {

		return getPersistence().findWithDynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * @see BasePersistence#update(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static RememberMeToken update(RememberMeToken rememberMeToken) {
		return getPersistence().update(rememberMeToken);
	}

	/**
	 * @see BasePersistence#update(com.liferay.portal.kernel.model.BaseModel, ServiceContext)
	 */
	public static RememberMeToken update(
		RememberMeToken rememberMeToken, ServiceContext serviceContext) {

		return getPersistence().update(rememberMeToken, serviceContext);
	}

	/**
	 * Returns the remember me token where RememberMeTokenId = &#63; or throws a <code>NoSuchRememberMeTokenException</code> if it could not be found.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the matching remember me token
	 * @throws NoSuchRememberMeTokenException if a matching remember me token could not be found
	 */
	public static RememberMeToken findByRememberMeTokenId(
			long RememberMeTokenId)
		throws com.liferay.portal.kernel.exception.
			NoSuchRememberMeTokenException {

		return getPersistence().findByRememberMeTokenId(RememberMeTokenId);
	}

	/**
	 * Returns the remember me token where RememberMeTokenId = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	public static RememberMeToken fetchByRememberMeTokenId(
		long RememberMeTokenId) {

		return getPersistence().fetchByRememberMeTokenId(RememberMeTokenId);
	}

	/**
	 * Returns the remember me token where RememberMeTokenId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	public static RememberMeToken fetchByRememberMeTokenId(
		long RememberMeTokenId, boolean useFinderCache) {

		return getPersistence().fetchByRememberMeTokenId(
			RememberMeTokenId, useFinderCache);
	}

	/**
	 * Removes the remember me token where RememberMeTokenId = &#63; from the database.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the remember me token that was removed
	 */
	public static RememberMeToken removeByRememberMeTokenId(
			long RememberMeTokenId)
		throws com.liferay.portal.kernel.exception.
			NoSuchRememberMeTokenException {

		return getPersistence().removeByRememberMeTokenId(RememberMeTokenId);
	}

	/**
	 * Returns the number of remember me tokens where RememberMeTokenId = &#63;.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the number of matching remember me tokens
	 */
	public static int countByRememberMeTokenId(long RememberMeTokenId) {
		return getPersistence().countByRememberMeTokenId(RememberMeTokenId);
	}

	/**
	 * Returns all the remember me tokens where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @return the matching remember me tokens
	 */
	public static List<RememberMeToken> findByC_U(long companyId, long userId) {
		return getPersistence().findByC_U(companyId, userId);
	}

	/**
	 * Returns a range of all the remember me tokens where companyId = &#63; and userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RememberMeTokenModelImpl</code>.
	 * </p>
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param start the lower bound of the range of remember me tokens
	 * @param end the upper bound of the range of remember me tokens (not inclusive)
	 * @return the range of matching remember me tokens
	 */
	public static List<RememberMeToken> findByC_U(
		long companyId, long userId, int start, int end) {

		return getPersistence().findByC_U(companyId, userId, start, end);
	}

	/**
	 * Returns an ordered range of all the remember me tokens where companyId = &#63; and userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RememberMeTokenModelImpl</code>.
	 * </p>
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param start the lower bound of the range of remember me tokens
	 * @param end the upper bound of the range of remember me tokens (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching remember me tokens
	 */
	public static List<RememberMeToken> findByC_U(
		long companyId, long userId, int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator) {

		return getPersistence().findByC_U(
			companyId, userId, start, end, orderByComparator);
	}

	/**
	 * Returns an ordered range of all the remember me tokens where companyId = &#63; and userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RememberMeTokenModelImpl</code>.
	 * </p>
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param start the lower bound of the range of remember me tokens
	 * @param end the upper bound of the range of remember me tokens (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of matching remember me tokens
	 */
	public static List<RememberMeToken> findByC_U(
		long companyId, long userId, int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator,
		boolean useFinderCache) {

		return getPersistence().findByC_U(
			companyId, userId, start, end, orderByComparator, useFinderCache);
	}

	/**
	 * Returns the first remember me token in the ordered set where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching remember me token
	 * @throws NoSuchRememberMeTokenException if a matching remember me token could not be found
	 */
	public static RememberMeToken findByC_U_First(
			long companyId, long userId,
			OrderByComparator<RememberMeToken> orderByComparator)
		throws com.liferay.portal.kernel.exception.
			NoSuchRememberMeTokenException {

		return getPersistence().findByC_U_First(
			companyId, userId, orderByComparator);
	}

	/**
	 * Returns the first remember me token in the ordered set where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	public static RememberMeToken fetchByC_U_First(
		long companyId, long userId,
		OrderByComparator<RememberMeToken> orderByComparator) {

		return getPersistence().fetchByC_U_First(
			companyId, userId, orderByComparator);
	}

	/**
	 * Returns the last remember me token in the ordered set where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching remember me token
	 * @throws NoSuchRememberMeTokenException if a matching remember me token could not be found
	 */
	public static RememberMeToken findByC_U_Last(
			long companyId, long userId,
			OrderByComparator<RememberMeToken> orderByComparator)
		throws com.liferay.portal.kernel.exception.
			NoSuchRememberMeTokenException {

		return getPersistence().findByC_U_Last(
			companyId, userId, orderByComparator);
	}

	/**
	 * Returns the last remember me token in the ordered set where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	public static RememberMeToken fetchByC_U_Last(
		long companyId, long userId,
		OrderByComparator<RememberMeToken> orderByComparator) {

		return getPersistence().fetchByC_U_Last(
			companyId, userId, orderByComparator);
	}

	/**
	 * Returns the remember me tokens before and after the current remember me token in the ordered set where companyId = &#63; and userId = &#63;.
	 *
	 * @param RememberMeTokenId the primary key of the current remember me token
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the previous, current, and next remember me token
	 * @throws NoSuchRememberMeTokenException if a remember me token with the primary key could not be found
	 */
	public static RememberMeToken[] findByC_U_PrevAndNext(
			long RememberMeTokenId, long companyId, long userId,
			OrderByComparator<RememberMeToken> orderByComparator)
		throws com.liferay.portal.kernel.exception.
			NoSuchRememberMeTokenException {

		return getPersistence().findByC_U_PrevAndNext(
			RememberMeTokenId, companyId, userId, orderByComparator);
	}

	/**
	 * Removes all the remember me tokens where companyId = &#63; and userId = &#63; from the database.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 */
	public static void removeByC_U(long companyId, long userId) {
		getPersistence().removeByC_U(companyId, userId);
	}

	/**
	 * Returns the number of remember me tokens where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @return the number of matching remember me tokens
	 */
	public static int countByC_U(long companyId, long userId) {
		return getPersistence().countByC_U(companyId, userId);
	}

	/**
	 * Caches the remember me token in the entity cache if it is enabled.
	 *
	 * @param rememberMeToken the remember me token
	 */
	public static void cacheResult(RememberMeToken rememberMeToken) {
		getPersistence().cacheResult(rememberMeToken);
	}

	/**
	 * Caches the remember me tokens in the entity cache if it is enabled.
	 *
	 * @param rememberMeTokens the remember me tokens
	 */
	public static void cacheResult(List<RememberMeToken> rememberMeTokens) {
		getPersistence().cacheResult(rememberMeTokens);
	}

	/**
	 * Creates a new remember me token with the primary key. Does not add the remember me token to the database.
	 *
	 * @param RememberMeTokenId the primary key for the new remember me token
	 * @return the new remember me token
	 */
	public static RememberMeToken create(long RememberMeTokenId) {
		return getPersistence().create(RememberMeTokenId);
	}

	/**
	 * Removes the remember me token with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param RememberMeTokenId the primary key of the remember me token
	 * @return the remember me token that was removed
	 * @throws NoSuchRememberMeTokenException if a remember me token with the primary key could not be found
	 */
	public static RememberMeToken remove(long RememberMeTokenId)
		throws com.liferay.portal.kernel.exception.
			NoSuchRememberMeTokenException {

		return getPersistence().remove(RememberMeTokenId);
	}

	public static RememberMeToken updateImpl(RememberMeToken rememberMeToken) {
		return getPersistence().updateImpl(rememberMeToken);
	}

	/**
	 * Returns the remember me token with the primary key or throws a <code>NoSuchRememberMeTokenException</code> if it could not be found.
	 *
	 * @param RememberMeTokenId the primary key of the remember me token
	 * @return the remember me token
	 * @throws NoSuchRememberMeTokenException if a remember me token with the primary key could not be found
	 */
	public static RememberMeToken findByPrimaryKey(long RememberMeTokenId)
		throws com.liferay.portal.kernel.exception.
			NoSuchRememberMeTokenException {

		return getPersistence().findByPrimaryKey(RememberMeTokenId);
	}

	/**
	 * Returns the remember me token with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param RememberMeTokenId the primary key of the remember me token
	 * @return the remember me token, or <code>null</code> if a remember me token with the primary key could not be found
	 */
	public static RememberMeToken fetchByPrimaryKey(long RememberMeTokenId) {
		return getPersistence().fetchByPrimaryKey(RememberMeTokenId);
	}

	/**
	 * Returns all the remember me tokens.
	 *
	 * @return the remember me tokens
	 */
	public static List<RememberMeToken> findAll() {
		return getPersistence().findAll();
	}

	/**
	 * Returns a range of all the remember me tokens.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RememberMeTokenModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of remember me tokens
	 * @param end the upper bound of the range of remember me tokens (not inclusive)
	 * @return the range of remember me tokens
	 */
	public static List<RememberMeToken> findAll(int start, int end) {
		return getPersistence().findAll(start, end);
	}

	/**
	 * Returns an ordered range of all the remember me tokens.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RememberMeTokenModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of remember me tokens
	 * @param end the upper bound of the range of remember me tokens (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of remember me tokens
	 */
	public static List<RememberMeToken> findAll(
		int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator) {

		return getPersistence().findAll(start, end, orderByComparator);
	}

	/**
	 * Returns an ordered range of all the remember me tokens.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>RememberMeTokenModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of remember me tokens
	 * @param end the upper bound of the range of remember me tokens (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of remember me tokens
	 */
	public static List<RememberMeToken> findAll(
		int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator,
		boolean useFinderCache) {

		return getPersistence().findAll(
			start, end, orderByComparator, useFinderCache);
	}

	/**
	 * Removes all the remember me tokens from the database.
	 */
	public static void removeAll() {
		getPersistence().removeAll();
	}

	/**
	 * Returns the number of remember me tokens.
	 *
	 * @return the number of remember me tokens
	 */
	public static int countAll() {
		return getPersistence().countAll();
	}

	public static RememberMeTokenPersistence getPersistence() {
		return _persistence;
	}

	public static void setPersistence(RememberMeTokenPersistence persistence) {
		_persistence = persistence;
	}

	private static volatile RememberMeTokenPersistence _persistence;

}