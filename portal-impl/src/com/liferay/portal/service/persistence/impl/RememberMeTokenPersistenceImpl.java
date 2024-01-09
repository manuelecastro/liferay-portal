/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.service.persistence.impl;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.EntityCacheUtil;
import com.liferay.portal.kernel.dao.orm.FinderCache;
import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.dao.orm.FinderPath;
import com.liferay.portal.kernel.dao.orm.Query;
import com.liferay.portal.kernel.dao.orm.QueryPos;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.exception.NoSuchRememberMeTokenException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.RememberMeToken;
import com.liferay.portal.kernel.model.RememberMeTokenTable;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.persistence.RememberMeTokenPersistence;
import com.liferay.portal.kernel.service.persistence.RememberMeTokenUtil;
import com.liferay.portal.kernel.service.persistence.impl.BasePersistenceImpl;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.model.impl.RememberMeTokenImpl;
import com.liferay.portal.model.impl.RememberMeTokenModelImpl;

import java.io.Serializable;

import java.lang.reflect.InvocationHandler;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The persistence implementation for the remember me token service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
public class RememberMeTokenPersistenceImpl
	extends BasePersistenceImpl<RememberMeToken>
	implements RememberMeTokenPersistence {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Always use <code>RememberMeTokenUtil</code> to access the remember me token persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static final String FINDER_CLASS_NAME_ENTITY =
		RememberMeTokenImpl.class.getName();

	public static final String FINDER_CLASS_NAME_LIST_WITH_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List1";

	public static final String FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List2";

	private FinderPath _finderPathWithPaginationFindAll;
	private FinderPath _finderPathWithoutPaginationFindAll;
	private FinderPath _finderPathCountAll;
	private FinderPath _finderPathFetchByRememberMeTokenId;
	private FinderPath _finderPathCountByRememberMeTokenId;

	/**
	 * Returns the remember me token where RememberMeTokenId = &#63; or throws a <code>NoSuchRememberMeTokenException</code> if it could not be found.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the matching remember me token
	 * @throws NoSuchRememberMeTokenException if a matching remember me token could not be found
	 */
	@Override
	public RememberMeToken findByRememberMeTokenId(long RememberMeTokenId)
		throws NoSuchRememberMeTokenException {

		RememberMeToken rememberMeToken = fetchByRememberMeTokenId(
			RememberMeTokenId);

		if (rememberMeToken == null) {
			StringBundler sb = new StringBundler(4);

			sb.append(_NO_SUCH_ENTITY_WITH_KEY);

			sb.append("RememberMeTokenId=");
			sb.append(RememberMeTokenId);

			sb.append("}");

			if (_log.isDebugEnabled()) {
				_log.debug(sb.toString());
			}

			throw new NoSuchRememberMeTokenException(sb.toString());
		}

		return rememberMeToken;
	}

	/**
	 * Returns the remember me token where RememberMeTokenId = &#63; or returns <code>null</code> if it could not be found. Uses the finder cache.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	@Override
	public RememberMeToken fetchByRememberMeTokenId(long RememberMeTokenId) {
		return fetchByRememberMeTokenId(RememberMeTokenId, true);
	}

	/**
	 * Returns the remember me token where RememberMeTokenId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	@Override
	public RememberMeToken fetchByRememberMeTokenId(
		long RememberMeTokenId, boolean useFinderCache) {

		Object[] finderArgs = null;

		if (useFinderCache) {
			finderArgs = new Object[] {RememberMeTokenId};
		}

		Object result = null;

		if (useFinderCache) {
			result = FinderCacheUtil.getResult(
				_finderPathFetchByRememberMeTokenId, finderArgs, this);
		}

		if (result instanceof RememberMeToken) {
			RememberMeToken rememberMeToken = (RememberMeToken)result;

			if (RememberMeTokenId != rememberMeToken.getRememberMeTokenId()) {
				result = null;
			}
		}

		if (result == null) {
			StringBundler sb = new StringBundler(3);

			sb.append(_SQL_SELECT_REMEMBERMETOKEN_WHERE);

			sb.append(_FINDER_COLUMN_REMEMBERMETOKENID_REMEMBERMETOKENID_2);

			String sql = sb.toString();

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				QueryPos queryPos = QueryPos.getInstance(query);

				queryPos.add(RememberMeTokenId);

				List<RememberMeToken> list = query.list();

				if (list.isEmpty()) {
					if (useFinderCache) {
						FinderCacheUtil.putResult(
							_finderPathFetchByRememberMeTokenId, finderArgs,
							list);
					}
				}
				else {
					if (list.size() > 1) {
						Collections.sort(list, Collections.reverseOrder());

						if (_log.isWarnEnabled()) {
							if (!useFinderCache) {
								finderArgs = new Object[] {RememberMeTokenId};
							}

							_log.warn(
								"RememberMeTokenPersistenceImpl.fetchByRememberMeTokenId(long, boolean) with parameters (" +
									StringUtil.merge(finderArgs) +
										") yields a result set with more than 1 result. This violates the logical unique restriction. There is no order guarantee on which result is returned by this finder.");
						}
					}

					RememberMeToken rememberMeToken = list.get(0);

					result = rememberMeToken;

					cacheResult(rememberMeToken);
				}
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		if (result instanceof List<?>) {
			return null;
		}
		else {
			return (RememberMeToken)result;
		}
	}

	/**
	 * Removes the remember me token where RememberMeTokenId = &#63; from the database.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the remember me token that was removed
	 */
	@Override
	public RememberMeToken removeByRememberMeTokenId(long RememberMeTokenId)
		throws NoSuchRememberMeTokenException {

		RememberMeToken rememberMeToken = findByRememberMeTokenId(
			RememberMeTokenId);

		return remove(rememberMeToken);
	}

	/**
	 * Returns the number of remember me tokens where RememberMeTokenId = &#63;.
	 *
	 * @param RememberMeTokenId the remember me token ID
	 * @return the number of matching remember me tokens
	 */
	@Override
	public int countByRememberMeTokenId(long RememberMeTokenId) {
		FinderPath finderPath = _finderPathCountByRememberMeTokenId;

		Object[] finderArgs = new Object[] {RememberMeTokenId};

		Long count = (Long)FinderCacheUtil.getResult(
			finderPath, finderArgs, this);

		if (count == null) {
			StringBundler sb = new StringBundler(2);

			sb.append(_SQL_COUNT_REMEMBERMETOKEN_WHERE);

			sb.append(_FINDER_COLUMN_REMEMBERMETOKENID_REMEMBERMETOKENID_2);

			String sql = sb.toString();

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				QueryPos queryPos = QueryPos.getInstance(query);

				queryPos.add(RememberMeTokenId);

				count = (Long)query.uniqueResult();

				FinderCacheUtil.putResult(finderPath, finderArgs, count);
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return count.intValue();
	}

	private static final String
		_FINDER_COLUMN_REMEMBERMETOKENID_REMEMBERMETOKENID_2 =
			"rememberMeToken.RememberMeTokenId = ?";

	private FinderPath _finderPathWithPaginationFindByC_U;
	private FinderPath _finderPathWithoutPaginationFindByC_U;
	private FinderPath _finderPathCountByC_U;

	/**
	 * Returns all the remember me tokens where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @return the matching remember me tokens
	 */
	@Override
	public List<RememberMeToken> findByC_U(long companyId, long userId) {
		return findByC_U(
			companyId, userId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
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
	@Override
	public List<RememberMeToken> findByC_U(
		long companyId, long userId, int start, int end) {

		return findByC_U(companyId, userId, start, end, null);
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
	@Override
	public List<RememberMeToken> findByC_U(
		long companyId, long userId, int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator) {

		return findByC_U(
			companyId, userId, start, end, orderByComparator, true);
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
	@Override
	public List<RememberMeToken> findByC_U(
		long companyId, long userId, int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator,
		boolean useFinderCache) {

		FinderPath finderPath = null;
		Object[] finderArgs = null;

		if ((start == QueryUtil.ALL_POS) && (end == QueryUtil.ALL_POS) &&
			(orderByComparator == null)) {

			if (useFinderCache) {
				finderPath = _finderPathWithoutPaginationFindByC_U;
				finderArgs = new Object[] {companyId, userId};
			}
		}
		else if (useFinderCache) {
			finderPath = _finderPathWithPaginationFindByC_U;
			finderArgs = new Object[] {
				companyId, userId, start, end, orderByComparator
			};
		}

		List<RememberMeToken> list = null;

		if (useFinderCache) {
			list = (List<RememberMeToken>)FinderCacheUtil.getResult(
				finderPath, finderArgs, this);

			if ((list != null) && !list.isEmpty()) {
				for (RememberMeToken rememberMeToken : list) {
					if ((companyId != rememberMeToken.getCompanyId()) ||
						(userId != rememberMeToken.getUserId())) {

						list = null;

						break;
					}
				}
			}
		}

		if (list == null) {
			StringBundler sb = null;

			if (orderByComparator != null) {
				sb = new StringBundler(
					4 + (orderByComparator.getOrderByFields().length * 2));
			}
			else {
				sb = new StringBundler(4);
			}

			sb.append(_SQL_SELECT_REMEMBERMETOKEN_WHERE);

			sb.append(_FINDER_COLUMN_C_U_COMPANYID_2);

			sb.append(_FINDER_COLUMN_C_U_USERID_2);

			if (orderByComparator != null) {
				appendOrderByComparator(
					sb, _ORDER_BY_ENTITY_ALIAS, orderByComparator);
			}
			else {
				sb.append(RememberMeTokenModelImpl.ORDER_BY_JPQL);
			}

			String sql = sb.toString();

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				QueryPos queryPos = QueryPos.getInstance(query);

				queryPos.add(companyId);

				queryPos.add(userId);

				list = (List<RememberMeToken>)QueryUtil.list(
					query, getDialect(), start, end);

				cacheResult(list);

				if (useFinderCache) {
					FinderCacheUtil.putResult(finderPath, finderArgs, list);
				}
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return list;
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
	@Override
	public RememberMeToken findByC_U_First(
			long companyId, long userId,
			OrderByComparator<RememberMeToken> orderByComparator)
		throws NoSuchRememberMeTokenException {

		RememberMeToken rememberMeToken = fetchByC_U_First(
			companyId, userId, orderByComparator);

		if (rememberMeToken != null) {
			return rememberMeToken;
		}

		StringBundler sb = new StringBundler(6);

		sb.append(_NO_SUCH_ENTITY_WITH_KEY);

		sb.append("companyId=");
		sb.append(companyId);

		sb.append(", userId=");
		sb.append(userId);

		sb.append("}");

		throw new NoSuchRememberMeTokenException(sb.toString());
	}

	/**
	 * Returns the first remember me token in the ordered set where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	@Override
	public RememberMeToken fetchByC_U_First(
		long companyId, long userId,
		OrderByComparator<RememberMeToken> orderByComparator) {

		List<RememberMeToken> list = findByC_U(
			companyId, userId, 0, 1, orderByComparator);

		if (!list.isEmpty()) {
			return list.get(0);
		}

		return null;
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
	@Override
	public RememberMeToken findByC_U_Last(
			long companyId, long userId,
			OrderByComparator<RememberMeToken> orderByComparator)
		throws NoSuchRememberMeTokenException {

		RememberMeToken rememberMeToken = fetchByC_U_Last(
			companyId, userId, orderByComparator);

		if (rememberMeToken != null) {
			return rememberMeToken;
		}

		StringBundler sb = new StringBundler(6);

		sb.append(_NO_SUCH_ENTITY_WITH_KEY);

		sb.append("companyId=");
		sb.append(companyId);

		sb.append(", userId=");
		sb.append(userId);

		sb.append("}");

		throw new NoSuchRememberMeTokenException(sb.toString());
	}

	/**
	 * Returns the last remember me token in the ordered set where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching remember me token, or <code>null</code> if a matching remember me token could not be found
	 */
	@Override
	public RememberMeToken fetchByC_U_Last(
		long companyId, long userId,
		OrderByComparator<RememberMeToken> orderByComparator) {

		int count = countByC_U(companyId, userId);

		if (count == 0) {
			return null;
		}

		List<RememberMeToken> list = findByC_U(
			companyId, userId, count - 1, count, orderByComparator);

		if (!list.isEmpty()) {
			return list.get(0);
		}

		return null;
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
	@Override
	public RememberMeToken[] findByC_U_PrevAndNext(
			long RememberMeTokenId, long companyId, long userId,
			OrderByComparator<RememberMeToken> orderByComparator)
		throws NoSuchRememberMeTokenException {

		RememberMeToken rememberMeToken = findByPrimaryKey(RememberMeTokenId);

		Session session = null;

		try {
			session = openSession();

			RememberMeToken[] array = new RememberMeTokenImpl[3];

			array[0] = getByC_U_PrevAndNext(
				session, rememberMeToken, companyId, userId, orderByComparator,
				true);

			array[1] = rememberMeToken;

			array[2] = getByC_U_PrevAndNext(
				session, rememberMeToken, companyId, userId, orderByComparator,
				false);

			return array;
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}
	}

	protected RememberMeToken getByC_U_PrevAndNext(
		Session session, RememberMeToken rememberMeToken, long companyId,
		long userId, OrderByComparator<RememberMeToken> orderByComparator,
		boolean previous) {

		StringBundler sb = null;

		if (orderByComparator != null) {
			sb = new StringBundler(
				5 + (orderByComparator.getOrderByConditionFields().length * 3) +
					(orderByComparator.getOrderByFields().length * 3));
		}
		else {
			sb = new StringBundler(4);
		}

		sb.append(_SQL_SELECT_REMEMBERMETOKEN_WHERE);

		sb.append(_FINDER_COLUMN_C_U_COMPANYID_2);

		sb.append(_FINDER_COLUMN_C_U_USERID_2);

		if (orderByComparator != null) {
			String[] orderByConditionFields =
				orderByComparator.getOrderByConditionFields();

			if (orderByConditionFields.length > 0) {
				sb.append(WHERE_AND);
			}

			for (int i = 0; i < orderByConditionFields.length; i++) {
				sb.append(_ORDER_BY_ENTITY_ALIAS);
				sb.append(orderByConditionFields[i]);

				if ((i + 1) < orderByConditionFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(WHERE_GREATER_THAN_HAS_NEXT);
					}
					else {
						sb.append(WHERE_LESSER_THAN_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(WHERE_GREATER_THAN);
					}
					else {
						sb.append(WHERE_LESSER_THAN);
					}
				}
			}

			sb.append(ORDER_BY_CLAUSE);

			String[] orderByFields = orderByComparator.getOrderByFields();

			for (int i = 0; i < orderByFields.length; i++) {
				sb.append(_ORDER_BY_ENTITY_ALIAS);
				sb.append(orderByFields[i]);

				if ((i + 1) < orderByFields.length) {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(ORDER_BY_ASC_HAS_NEXT);
					}
					else {
						sb.append(ORDER_BY_DESC_HAS_NEXT);
					}
				}
				else {
					if (orderByComparator.isAscending() ^ previous) {
						sb.append(ORDER_BY_ASC);
					}
					else {
						sb.append(ORDER_BY_DESC);
					}
				}
			}
		}
		else {
			sb.append(RememberMeTokenModelImpl.ORDER_BY_JPQL);
		}

		String sql = sb.toString();

		Query query = session.createQuery(sql);

		query.setFirstResult(0);
		query.setMaxResults(2);

		QueryPos queryPos = QueryPos.getInstance(query);

		queryPos.add(companyId);

		queryPos.add(userId);

		if (orderByComparator != null) {
			for (Object orderByConditionValue :
					orderByComparator.getOrderByConditionValues(
						rememberMeToken)) {

				queryPos.add(orderByConditionValue);
			}
		}

		List<RememberMeToken> list = query.list();

		if (list.size() == 2) {
			return list.get(1);
		}
		else {
			return null;
		}
	}

	/**
	 * Removes all the remember me tokens where companyId = &#63; and userId = &#63; from the database.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 */
	@Override
	public void removeByC_U(long companyId, long userId) {
		for (RememberMeToken rememberMeToken :
				findByC_U(
					companyId, userId, QueryUtil.ALL_POS, QueryUtil.ALL_POS,
					null)) {

			remove(rememberMeToken);
		}
	}

	/**
	 * Returns the number of remember me tokens where companyId = &#63; and userId = &#63;.
	 *
	 * @param companyId the company ID
	 * @param userId the user ID
	 * @return the number of matching remember me tokens
	 */
	@Override
	public int countByC_U(long companyId, long userId) {
		FinderPath finderPath = _finderPathCountByC_U;

		Object[] finderArgs = new Object[] {companyId, userId};

		Long count = (Long)FinderCacheUtil.getResult(
			finderPath, finderArgs, this);

		if (count == null) {
			StringBundler sb = new StringBundler(3);

			sb.append(_SQL_COUNT_REMEMBERMETOKEN_WHERE);

			sb.append(_FINDER_COLUMN_C_U_COMPANYID_2);

			sb.append(_FINDER_COLUMN_C_U_USERID_2);

			String sql = sb.toString();

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				QueryPos queryPos = QueryPos.getInstance(query);

				queryPos.add(companyId);

				queryPos.add(userId);

				count = (Long)query.uniqueResult();

				FinderCacheUtil.putResult(finderPath, finderArgs, count);
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return count.intValue();
	}

	private static final String _FINDER_COLUMN_C_U_COMPANYID_2 =
		"rememberMeToken.companyId = ? AND ";

	private static final String _FINDER_COLUMN_C_U_USERID_2 =
		"rememberMeToken.userId = ?";

	public RememberMeTokenPersistenceImpl() {
		setModelClass(RememberMeToken.class);

		setModelImplClass(RememberMeTokenImpl.class);
		setModelPKClass(long.class);

		setTable(RememberMeTokenTable.INSTANCE);
	}

	/**
	 * Caches the remember me token in the entity cache if it is enabled.
	 *
	 * @param rememberMeToken the remember me token
	 */
	@Override
	public void cacheResult(RememberMeToken rememberMeToken) {
		EntityCacheUtil.putResult(
			RememberMeTokenImpl.class, rememberMeToken.getPrimaryKey(),
			rememberMeToken);

		FinderCacheUtil.putResult(
			_finderPathFetchByRememberMeTokenId,
			new Object[] {rememberMeToken.getRememberMeTokenId()},
			rememberMeToken);
	}

	private int _valueObjectFinderCacheListThreshold;

	/**
	 * Caches the remember me tokens in the entity cache if it is enabled.
	 *
	 * @param rememberMeTokens the remember me tokens
	 */
	@Override
	public void cacheResult(List<RememberMeToken> rememberMeTokens) {
		if ((_valueObjectFinderCacheListThreshold == 0) ||
			((_valueObjectFinderCacheListThreshold > 0) &&
			 (rememberMeTokens.size() >
				 _valueObjectFinderCacheListThreshold))) {

			return;
		}

		for (RememberMeToken rememberMeToken : rememberMeTokens) {
			if (EntityCacheUtil.getResult(
					RememberMeTokenImpl.class,
					rememberMeToken.getPrimaryKey()) == null) {

				cacheResult(rememberMeToken);
			}
		}
	}

	/**
	 * Clears the cache for all remember me tokens.
	 *
	 * <p>
	 * The <code>EntityCache</code> and <code>FinderCache</code> are both cleared by this method.
	 * </p>
	 */
	@Override
	public void clearCache() {
		EntityCacheUtil.clearCache(RememberMeTokenImpl.class);

		FinderCacheUtil.clearCache(RememberMeTokenImpl.class);
	}

	/**
	 * Clears the cache for the remember me token.
	 *
	 * <p>
	 * The <code>EntityCache</code> and <code>FinderCache</code> are both cleared by this method.
	 * </p>
	 */
	@Override
	public void clearCache(RememberMeToken rememberMeToken) {
		EntityCacheUtil.removeResult(
			RememberMeTokenImpl.class, rememberMeToken);
	}

	@Override
	public void clearCache(List<RememberMeToken> rememberMeTokens) {
		for (RememberMeToken rememberMeToken : rememberMeTokens) {
			EntityCacheUtil.removeResult(
				RememberMeTokenImpl.class, rememberMeToken);
		}
	}

	@Override
	public void clearCache(Set<Serializable> primaryKeys) {
		FinderCacheUtil.clearCache(RememberMeTokenImpl.class);

		for (Serializable primaryKey : primaryKeys) {
			EntityCacheUtil.removeResult(RememberMeTokenImpl.class, primaryKey);
		}
	}

	protected void cacheUniqueFindersCache(
		RememberMeTokenModelImpl rememberMeTokenModelImpl) {

		Object[] args = new Object[] {
			rememberMeTokenModelImpl.getRememberMeTokenId()
		};

		FinderCacheUtil.putResult(
			_finderPathCountByRememberMeTokenId, args, Long.valueOf(1));
		FinderCacheUtil.putResult(
			_finderPathFetchByRememberMeTokenId, args,
			rememberMeTokenModelImpl);
	}

	/**
	 * Creates a new remember me token with the primary key. Does not add the remember me token to the database.
	 *
	 * @param RememberMeTokenId the primary key for the new remember me token
	 * @return the new remember me token
	 */
	@Override
	public RememberMeToken create(long RememberMeTokenId) {
		RememberMeToken rememberMeToken = new RememberMeTokenImpl();

		rememberMeToken.setNew(true);
		rememberMeToken.setPrimaryKey(RememberMeTokenId);

		rememberMeToken.setCompanyId(CompanyThreadLocal.getCompanyId());

		return rememberMeToken;
	}

	/**
	 * Removes the remember me token with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param RememberMeTokenId the primary key of the remember me token
	 * @return the remember me token that was removed
	 * @throws NoSuchRememberMeTokenException if a remember me token with the primary key could not be found
	 */
	@Override
	public RememberMeToken remove(long RememberMeTokenId)
		throws NoSuchRememberMeTokenException {

		return remove((Serializable)RememberMeTokenId);
	}

	/**
	 * Removes the remember me token with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param primaryKey the primary key of the remember me token
	 * @return the remember me token that was removed
	 * @throws NoSuchRememberMeTokenException if a remember me token with the primary key could not be found
	 */
	@Override
	public RememberMeToken remove(Serializable primaryKey)
		throws NoSuchRememberMeTokenException {

		Session session = null;

		try {
			session = openSession();

			RememberMeToken rememberMeToken = (RememberMeToken)session.get(
				RememberMeTokenImpl.class, primaryKey);

			if (rememberMeToken == null) {
				if (_log.isDebugEnabled()) {
					_log.debug(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
				}

				throw new NoSuchRememberMeTokenException(
					_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
			}

			return remove(rememberMeToken);
		}
		catch (NoSuchRememberMeTokenException noSuchEntityException) {
			throw noSuchEntityException;
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}
	}

	@Override
	protected RememberMeToken removeImpl(RememberMeToken rememberMeToken) {
		Session session = null;

		try {
			session = openSession();

			if (!session.contains(rememberMeToken)) {
				rememberMeToken = (RememberMeToken)session.get(
					RememberMeTokenImpl.class,
					rememberMeToken.getPrimaryKeyObj());
			}

			if (rememberMeToken != null) {
				session.delete(rememberMeToken);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		if (rememberMeToken != null) {
			clearCache(rememberMeToken);
		}

		return rememberMeToken;
	}

	@Override
	public RememberMeToken updateImpl(RememberMeToken rememberMeToken) {
		boolean isNew = rememberMeToken.isNew();

		if (!(rememberMeToken instanceof RememberMeTokenModelImpl)) {
			InvocationHandler invocationHandler = null;

			if (ProxyUtil.isProxyClass(rememberMeToken.getClass())) {
				invocationHandler = ProxyUtil.getInvocationHandler(
					rememberMeToken);

				throw new IllegalArgumentException(
					"Implement ModelWrapper in rememberMeToken proxy " +
						invocationHandler.getClass());
			}

			throw new IllegalArgumentException(
				"Implement ModelWrapper in custom RememberMeToken implementation " +
					rememberMeToken.getClass());
		}

		RememberMeTokenModelImpl rememberMeTokenModelImpl =
			(RememberMeTokenModelImpl)rememberMeToken;

		if (isNew && (rememberMeToken.getCreateDate() == null)) {
			ServiceContext serviceContext =
				ServiceContextThreadLocal.getServiceContext();

			Date date = new Date();

			if (serviceContext == null) {
				rememberMeToken.setCreateDate(date);
			}
			else {
				rememberMeToken.setCreateDate(
					serviceContext.getCreateDate(date));
			}
		}

		Session session = null;

		try {
			session = openSession();

			if (isNew) {
				session.save(rememberMeToken);
			}
			else {
				rememberMeToken = (RememberMeToken)session.merge(
					rememberMeToken);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		EntityCacheUtil.putResult(
			RememberMeTokenImpl.class, rememberMeTokenModelImpl, false, true);

		cacheUniqueFindersCache(rememberMeTokenModelImpl);

		if (isNew) {
			rememberMeToken.setNew(false);
		}

		rememberMeToken.resetOriginalValues();

		return rememberMeToken;
	}

	/**
	 * Returns the remember me token with the primary key or throws a <code>com.liferay.portal.kernel.exception.NoSuchModelException</code> if it could not be found.
	 *
	 * @param primaryKey the primary key of the remember me token
	 * @return the remember me token
	 * @throws NoSuchRememberMeTokenException if a remember me token with the primary key could not be found
	 */
	@Override
	public RememberMeToken findByPrimaryKey(Serializable primaryKey)
		throws NoSuchRememberMeTokenException {

		RememberMeToken rememberMeToken = fetchByPrimaryKey(primaryKey);

		if (rememberMeToken == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
			}

			throw new NoSuchRememberMeTokenException(
				_NO_SUCH_ENTITY_WITH_PRIMARY_KEY + primaryKey);
		}

		return rememberMeToken;
	}

	/**
	 * Returns the remember me token with the primary key or throws a <code>NoSuchRememberMeTokenException</code> if it could not be found.
	 *
	 * @param RememberMeTokenId the primary key of the remember me token
	 * @return the remember me token
	 * @throws NoSuchRememberMeTokenException if a remember me token with the primary key could not be found
	 */
	@Override
	public RememberMeToken findByPrimaryKey(long RememberMeTokenId)
		throws NoSuchRememberMeTokenException {

		return findByPrimaryKey((Serializable)RememberMeTokenId);
	}

	/**
	 * Returns the remember me token with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param RememberMeTokenId the primary key of the remember me token
	 * @return the remember me token, or <code>null</code> if a remember me token with the primary key could not be found
	 */
	@Override
	public RememberMeToken fetchByPrimaryKey(long RememberMeTokenId) {
		return fetchByPrimaryKey((Serializable)RememberMeTokenId);
	}

	/**
	 * Returns all the remember me tokens.
	 *
	 * @return the remember me tokens
	 */
	@Override
	public List<RememberMeToken> findAll() {
		return findAll(QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
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
	@Override
	public List<RememberMeToken> findAll(int start, int end) {
		return findAll(start, end, null);
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
	@Override
	public List<RememberMeToken> findAll(
		int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator) {

		return findAll(start, end, orderByComparator, true);
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
	@Override
	public List<RememberMeToken> findAll(
		int start, int end,
		OrderByComparator<RememberMeToken> orderByComparator,
		boolean useFinderCache) {

		FinderPath finderPath = null;
		Object[] finderArgs = null;

		if ((start == QueryUtil.ALL_POS) && (end == QueryUtil.ALL_POS) &&
			(orderByComparator == null)) {

			if (useFinderCache) {
				finderPath = _finderPathWithoutPaginationFindAll;
				finderArgs = FINDER_ARGS_EMPTY;
			}
		}
		else if (useFinderCache) {
			finderPath = _finderPathWithPaginationFindAll;
			finderArgs = new Object[] {start, end, orderByComparator};
		}

		List<RememberMeToken> list = null;

		if (useFinderCache) {
			list = (List<RememberMeToken>)FinderCacheUtil.getResult(
				finderPath, finderArgs, this);
		}

		if (list == null) {
			StringBundler sb = null;
			String sql = null;

			if (orderByComparator != null) {
				sb = new StringBundler(
					2 + (orderByComparator.getOrderByFields().length * 2));

				sb.append(_SQL_SELECT_REMEMBERMETOKEN);

				appendOrderByComparator(
					sb, _ORDER_BY_ENTITY_ALIAS, orderByComparator);

				sql = sb.toString();
			}
			else {
				sql = _SQL_SELECT_REMEMBERMETOKEN;

				sql = sql.concat(RememberMeTokenModelImpl.ORDER_BY_JPQL);
			}

			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(sql);

				list = (List<RememberMeToken>)QueryUtil.list(
					query, getDialect(), start, end);

				cacheResult(list);

				if (useFinderCache) {
					FinderCacheUtil.putResult(finderPath, finderArgs, list);
				}
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return list;
	}

	/**
	 * Removes all the remember me tokens from the database.
	 *
	 */
	@Override
	public void removeAll() {
		for (RememberMeToken rememberMeToken : findAll()) {
			remove(rememberMeToken);
		}
	}

	/**
	 * Returns the number of remember me tokens.
	 *
	 * @return the number of remember me tokens
	 */
	@Override
	public int countAll() {
		Long count = (Long)FinderCacheUtil.getResult(
			_finderPathCountAll, FINDER_ARGS_EMPTY, this);

		if (count == null) {
			Session session = null;

			try {
				session = openSession();

				Query query = session.createQuery(_SQL_COUNT_REMEMBERMETOKEN);

				count = (Long)query.uniqueResult();

				FinderCacheUtil.putResult(
					_finderPathCountAll, FINDER_ARGS_EMPTY, count);
			}
			catch (Exception exception) {
				throw processException(exception);
			}
			finally {
				closeSession(session);
			}
		}

		return count.intValue();
	}

	@Override
	protected EntityCache getEntityCache() {
		return EntityCacheUtil.getEntityCache();
	}

	@Override
	protected String getPKDBName() {
		return "RememberMeTokenId";
	}

	@Override
	protected String getSelectSQL() {
		return _SQL_SELECT_REMEMBERMETOKEN;
	}

	@Override
	protected Map<String, Integer> getTableColumnsMap() {
		return RememberMeTokenModelImpl.TABLE_COLUMNS_MAP;
	}

	/**
	 * Initializes the remember me token persistence.
	 */
	public void afterPropertiesSet() {
		_valueObjectFinderCacheListThreshold = GetterUtil.getInteger(
			PropsUtil.get(PropsKeys.VALUE_OBJECT_FINDER_CACHE_LIST_THRESHOLD));

		_finderPathWithPaginationFindAll = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITH_PAGINATION, "findAll", new String[0],
			new String[0], true);

		_finderPathWithoutPaginationFindAll = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "findAll", new String[0],
			new String[0], true);

		_finderPathCountAll = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "countAll",
			new String[0], new String[0], false);

		_finderPathFetchByRememberMeTokenId = new FinderPath(
			FINDER_CLASS_NAME_ENTITY, "fetchByRememberMeTokenId",
			new String[] {Long.class.getName()},
			new String[] {"RememberMeTokenId"}, true);

		_finderPathCountByRememberMeTokenId = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION,
			"countByRememberMeTokenId", new String[] {Long.class.getName()},
			new String[] {"RememberMeTokenId"}, false);

		_finderPathWithPaginationFindByC_U = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITH_PAGINATION, "findByC_U",
			new String[] {
				Long.class.getName(), Long.class.getName(),
				Integer.class.getName(), Integer.class.getName(),
				OrderByComparator.class.getName()
			},
			new String[] {"companyId", "userId"}, true);

		_finderPathWithoutPaginationFindByC_U = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "findByC_U",
			new String[] {Long.class.getName(), Long.class.getName()},
			new String[] {"companyId", "userId"}, true);

		_finderPathCountByC_U = new FinderPath(
			FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION, "countByC_U",
			new String[] {Long.class.getName(), Long.class.getName()},
			new String[] {"companyId", "userId"}, false);

		RememberMeTokenUtil.setPersistence(this);
	}

	public void destroy() {
		RememberMeTokenUtil.setPersistence(null);

		EntityCacheUtil.removeCache(RememberMeTokenImpl.class.getName());
	}

	private static final String _SQL_SELECT_REMEMBERMETOKEN =
		"SELECT rememberMeToken FROM RememberMeToken rememberMeToken";

	private static final String _SQL_SELECT_REMEMBERMETOKEN_WHERE =
		"SELECT rememberMeToken FROM RememberMeToken rememberMeToken WHERE ";

	private static final String _SQL_COUNT_REMEMBERMETOKEN =
		"SELECT COUNT(rememberMeToken) FROM RememberMeToken rememberMeToken";

	private static final String _SQL_COUNT_REMEMBERMETOKEN_WHERE =
		"SELECT COUNT(rememberMeToken) FROM RememberMeToken rememberMeToken WHERE ";

	private static final String _ORDER_BY_ENTITY_ALIAS = "rememberMeToken.";

	private static final String _NO_SUCH_ENTITY_WITH_PRIMARY_KEY =
		"No RememberMeToken exists with the primary key ";

	private static final String _NO_SUCH_ENTITY_WITH_KEY =
		"No RememberMeToken exists with the key {";

	private static final Log _log = LogFactoryUtil.getLog(
		RememberMeTokenPersistenceImpl.class);

	@Override
	protected FinderCache getFinderCache() {
		return FinderCacheUtil.getFinderCache();
	}

}