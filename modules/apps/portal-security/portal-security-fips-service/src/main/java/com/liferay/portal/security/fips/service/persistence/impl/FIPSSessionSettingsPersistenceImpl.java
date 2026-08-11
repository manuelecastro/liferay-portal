/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.persistence.impl;

import com.liferay.portal.kernel.configuration.Configuration;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.FinderCache;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.SessionFactory;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.persistence.impl.BasePersistenceImpl;
import com.liferay.portal.kernel.service.persistence.impl.FinderColumn;
import com.liferay.portal.kernel.service.persistence.impl.UniquePersistenceFinder;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.security.fips.exception.NoSuchSessionSettingsException;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;
import com.liferay.portal.security.fips.model.FIPSSessionSettingsTable;
import com.liferay.portal.security.fips.model.impl.FIPSSessionSettingsImpl;
import com.liferay.portal.security.fips.model.impl.FIPSSessionSettingsModelImpl;
import com.liferay.portal.security.fips.service.persistence.FIPSSessionSettingsPersistence;
import com.liferay.portal.security.fips.service.persistence.FIPSSessionSettingsUtil;
import com.liferay.portal.security.fips.service.persistence.impl.constants.FIPSPersistenceConstants;

import java.io.Serializable;

import java.lang.reflect.InvocationHandler;

import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * The persistence implementation for the fips session settings service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
@Component(service = FIPSSessionSettingsPersistence.class)
public class FIPSSessionSettingsPersistenceImpl
	extends BasePersistenceImpl
		<FIPSSessionSettings, NoSuchSessionSettingsException>
	implements FIPSSessionSettingsPersistence {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Always use <code>FIPSSessionSettingsUtil</code> to access the fips session settings persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */
	public static final String FINDER_CLASS_NAME_ENTITY =
		FIPSSessionSettingsImpl.class.getName();

	public static final String FINDER_CLASS_NAME_LIST_WITH_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List1";

	public static final String FINDER_CLASS_NAME_LIST_WITHOUT_PAGINATION =
		FINDER_CLASS_NAME_ENTITY + ".List2";

	private UniquePersistenceFinder
		<FIPSSessionSettings, NoSuchSessionSettingsException>
			_uniquePersistenceFinderByCompanyId;

	/**
	 * Returns the fips session settings where companyId = &#63; or throws a <code>NoSuchSessionSettingsException</code> if it could not be found.
	 *
	 * @param companyId the company ID
	 * @return the matching fips session settings
	 * @throws NoSuchSessionSettingsException if a matching fips session settings could not be found
	 */
	@Override
	public FIPSSessionSettings findByCompanyId(long companyId)
		throws NoSuchSessionSettingsException {

		return _uniquePersistenceFinderByCompanyId.find(
			finderCache, new Object[] {companyId});
	}

	/**
	 * Returns the fips session settings where companyId = &#63; or returns <code>null</code> if it could not be found, optionally using the finder cache.
	 *
	 * @param companyId the company ID
	 * @param useFinderCache whether to use the finder cache
	 * @return the matching fips session settings, or <code>null</code> if a matching fips session settings could not be found
	 */
	@Override
	public FIPSSessionSettings fetchByCompanyId(
		long companyId, boolean useFinderCache) {

		return _uniquePersistenceFinderByCompanyId.fetch(
			finderCache, new Object[] {companyId}, useFinderCache);
	}

	/**
	 * Removes the fips session settings where companyId = &#63; from the database.
	 *
	 * @param companyId the company ID
	 * @return the fips session settings that was removed
	 */
	@Override
	public FIPSSessionSettings removeByCompanyId(long companyId)
		throws NoSuchSessionSettingsException {

		FIPSSessionSettings fipsSessionSettings = findByCompanyId(companyId);

		return remove(fipsSessionSettings);
	}

	/**
	 * Returns the number of fips session settingses where companyId = &#63;.
	 *
	 * @param companyId the company ID
	 * @return the number of matching fips session settingses
	 */
	@Override
	public int countByCompanyId(long companyId) {
		return _uniquePersistenceFinderByCompanyId.count(
			finderCache, new Object[] {companyId});
	}

	public FIPSSessionSettingsPersistenceImpl() {
		setModelClass(FIPSSessionSettings.class);

		setModelImplClass(FIPSSessionSettingsImpl.class);
		setModelPKClass(long.class);

		setTable(FIPSSessionSettingsTable.INSTANCE);
	}

	/**
	 * Creates a new fips session settings with the primary key. Does not add the fips session settings to the database.
	 *
	 * @param fipsSessionSettingsId the primary key for the new fips session settings
	 * @return the new fips session settings
	 */
	@Override
	public FIPSSessionSettings create(long fipsSessionSettingsId) {
		FIPSSessionSettings fipsSessionSettings = new FIPSSessionSettingsImpl();

		fipsSessionSettings.setNew(true);
		fipsSessionSettings.setPrimaryKey(fipsSessionSettingsId);

		fipsSessionSettings.setCompanyId(CompanyThreadLocal.getCompanyId());

		return fipsSessionSettings;
	}

	/**
	 * Removes the fips session settings with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings that was removed
	 * @throws NoSuchSessionSettingsException if a fips session settings with the primary key could not be found
	 */
	@Override
	public FIPSSessionSettings remove(long fipsSessionSettingsId)
		throws NoSuchSessionSettingsException {

		return remove((Serializable)fipsSessionSettingsId);
	}

	@Override
	protected FIPSSessionSettings removeImpl(
		FIPSSessionSettings fipsSessionSettings) {

		Session session = null;

		try {
			session = openSession();

			if (!session.contains(fipsSessionSettings)) {
				fipsSessionSettings = (FIPSSessionSettings)session.get(
					FIPSSessionSettingsImpl.class,
					fipsSessionSettings.getPrimaryKeyObj());
			}

			if (fipsSessionSettings != null) {
				session.delete(fipsSessionSettings);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		if (fipsSessionSettings != null) {
			clearCache(fipsSessionSettings);
		}

		return fipsSessionSettings;
	}

	@Override
	public FIPSSessionSettings updateImpl(
		FIPSSessionSettings fipsSessionSettings) {

		boolean isNew = fipsSessionSettings.isNew();

		if (!(fipsSessionSettings instanceof FIPSSessionSettingsModelImpl)) {
			InvocationHandler invocationHandler = null;

			if (ProxyUtil.isProxyClass(fipsSessionSettings.getClass())) {
				invocationHandler = ProxyUtil.getInvocationHandler(
					fipsSessionSettings);

				throw new IllegalArgumentException(
					"Implement ModelWrapper in fipsSessionSettings proxy " +
						invocationHandler.getClass());
			}

			throw new IllegalArgumentException(
				"Implement ModelWrapper in custom FIPSSessionSettings implementation " +
					fipsSessionSettings.getClass());
		}

		FIPSSessionSettingsModelImpl fipsSessionSettingsModelImpl =
			(FIPSSessionSettingsModelImpl)fipsSessionSettings;

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		Date date = new Date();

		if (isNew && (fipsSessionSettings.getCreateDate() == null)) {
			if (serviceContext == null) {
				fipsSessionSettings.setCreateDate(date);
			}
			else {
				fipsSessionSettings.setCreateDate(
					serviceContext.getCreateDate(date));
			}
		}

		if (!fipsSessionSettingsModelImpl.hasSetModifiedDate()) {
			if (serviceContext == null) {
				fipsSessionSettings.setModifiedDate(date);
			}
			else {
				fipsSessionSettings.setModifiedDate(
					serviceContext.getModifiedDate(date));
			}
		}

		Session session = null;

		try {
			session = openSession();

			if (isNew) {
				session.save(fipsSessionSettings);
			}
			else {
				fipsSessionSettings = (FIPSSessionSettings)session.merge(
					fipsSessionSettings);
			}
		}
		catch (Exception exception) {
			throw processException(exception);
		}
		finally {
			closeSession(session);
		}

		cacheUniqueFindersResult(fipsSessionSettings, false);

		if (isNew) {
			fipsSessionSettings.setNew(false);
		}

		fipsSessionSettings.resetOriginalValues();

		return fipsSessionSettings;
	}

	/**
	 * Returns the fips session settings with the primary key or throws a <code>NoSuchSessionSettingsException</code> if it could not be found.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings
	 * @throws NoSuchSessionSettingsException if a fips session settings with the primary key could not be found
	 */
	@Override
	public FIPSSessionSettings findByPrimaryKey(long fipsSessionSettingsId)
		throws NoSuchSessionSettingsException {

		return findByPrimaryKey((Serializable)fipsSessionSettingsId);
	}

	/**
	 * Returns the fips session settings with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param fipsSessionSettingsId the primary key of the fips session settings
	 * @return the fips session settings, or <code>null</code> if a fips session settings with the primary key could not be found
	 */
	@Override
	public FIPSSessionSettings fetchByPrimaryKey(long fipsSessionSettingsId) {
		return fetchByPrimaryKey((Serializable)fipsSessionSettingsId);
	}

	@Override
	protected EntityCache getEntityCache() {
		return entityCache;
	}

	@Override
	protected String getPKDBName() {
		return "fipsSessionSettingsId";
	}

	@Override
	protected String getSelectSQL() {
		return _SQL_SELECT_FIPSSESSIONSETTINGS;
	}

	@Override
	protected Map<String, Integer> getTableColumnsMap() {
		return FIPSSessionSettingsModelImpl.TABLE_COLUMNS_MAP;
	}

	/**
	 * Initializes the fips session settings persistence.
	 */
	@Activate
	public void activate() {
		_uniquePersistenceFinderByCompanyId = new UniquePersistenceFinder<>(
			this,
			createUniqueFinderPath(
				FINDER_CLASS_NAME_ENTITY, "fetchByCompanyId",
				new String[] {Long.class.getName()}, new String[] {"companyId"},
				0, 0, false, FIPSSessionSettings::getCompanyId),
			_SQL_SELECT_FIPSSESSIONSETTINGS_WHERE, "",
			new FinderColumn<>(
				"fipsSessionSettings.", "companyId", FinderColumn.Type.LONG,
				"=", true, true, FIPSSessionSettings::getCompanyId));

		FIPSSessionSettingsUtil.setPersistence(this);
	}

	@Deactivate
	public void deactivate() {
		FIPSSessionSettingsUtil.setPersistence(null);

		entityCache.removeCache(FIPSSessionSettingsImpl.class.getName());
	}

	@Override
	@Reference(
		target = FIPSPersistenceConstants.SERVICE_CONFIGURATION_FILTER,
		unbind = "-"
	)
	public void setConfiguration(Configuration configuration) {
	}

	@Override
	@Reference(
		target = FIPSPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
	}

	@Override
	@Reference(
		target = FIPSPersistenceConstants.ORIGIN_BUNDLE_SYMBOLIC_NAME_FILTER,
		unbind = "-"
	)
	public void setSessionFactory(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	@Reference
	protected EntityCache entityCache;

	@Reference
	protected FinderCache finderCache;

	private static final String _SQL_SELECT_FIPSSESSIONSETTINGS =
		"SELECT fipsSessionSettings FROM FIPSSessionSettings fipsSessionSettings";

	private static final String _SQL_SELECT_FIPSSESSIONSETTINGS_WHERE =
		"SELECT fipsSessionSettings FROM FIPSSessionSettings fipsSessionSettings WHERE ";

	@Override
	protected FinderCache getFinderCache() {
		return finderCache;
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:-807523402