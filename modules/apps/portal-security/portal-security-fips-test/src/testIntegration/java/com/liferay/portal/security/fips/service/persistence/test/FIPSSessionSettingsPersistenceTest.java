/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.service.persistence.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.util.IntegerWrapper;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderByComparatorFactoryUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.security.fips.exception.NoSuchSessionSettingsException;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;
import com.liferay.portal.security.fips.service.FIPSSessionSettingsLocalServiceUtil;
import com.liferay.portal.security.fips.service.persistence.FIPSSessionSettingsPersistence;
import com.liferay.portal.security.fips.service.persistence.FIPSSessionSettingsUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PersistenceTestRule;
import com.liferay.portal.test.rule.TransactionalTestRule;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @generated
 */
@RunWith(Arquillian.class)
public class FIPSSessionSettingsPersistenceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(), PersistenceTestRule.INSTANCE,
			new TransactionalTestRule(
				Propagation.REQUIRED,
				"com.liferay.portal.security.fips.service"));

	@Before
	public void setUp() {
		_persistence = FIPSSessionSettingsUtil.getPersistence();

		Class<?> clazz = _persistence.getClass();

		_dynamicQueryClassLoader = clazz.getClassLoader();
	}

	@After
	public void tearDown() throws Exception {
		Iterator<FIPSSessionSettings> iterator =
			_fipsSessionSettingses.iterator();

		while (iterator.hasNext()) {
			_persistence.remove(iterator.next());

			iterator.remove();
		}
	}

	@Test
	public void testCreate() throws Exception {
		long pk = RandomTestUtil.nextLong();

		FIPSSessionSettings fipsSessionSettings = _persistence.create(pk);

		Assert.assertNotNull(fipsSessionSettings);

		Assert.assertEquals(fipsSessionSettings.getPrimaryKey(), pk);
	}

	@Test
	public void testRemove() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		_persistence.remove(newFIPSSessionSettings);

		FIPSSessionSettings existingFIPSSessionSettings =
			_persistence.fetchByPrimaryKey(
				newFIPSSessionSettings.getPrimaryKey());

		Assert.assertNull(existingFIPSSessionSettings);
	}

	@Test
	public void testUpdateNew() throws Exception {
		addFIPSSessionSettings();
	}

	@Test
	public void testUpdateExisting() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		newFIPSSessionSettings.setCompanyId(RandomTestUtil.nextLong());

		newFIPSSessionSettings.setUserId(RandomTestUtil.nextLong());

		newFIPSSessionSettings.setUserName(RandomTestUtil.randomString());

		newFIPSSessionSettings.setCreateDate(RandomTestUtil.nextDate());

		newFIPSSessionSettings.setModifiedDate(RandomTestUtil.nextDate());

		newFIPSSessionSettings.setAbsoluteLifetimeMinutes(
			RandomTestUtil.nextInt());

		newFIPSSessionSettings.setIdleTimeoutMinutes(RandomTestUtil.nextInt());

		newFIPSSessionSettings = _persistence.update(newFIPSSessionSettings);

		_fipsSessionSettingses.add(newFIPSSessionSettings);

		FIPSSessionSettings existingFIPSSessionSettings =
			_persistence.findByPrimaryKey(
				newFIPSSessionSettings.getPrimaryKey());

		Assert.assertEquals(
			existingFIPSSessionSettings.getMvccVersion(),
			newFIPSSessionSettings.getMvccVersion());
		Assert.assertEquals(
			existingFIPSSessionSettings.getFipsSessionSettingsId(),
			newFIPSSessionSettings.getFipsSessionSettingsId());
		Assert.assertEquals(
			existingFIPSSessionSettings.getCompanyId(),
			newFIPSSessionSettings.getCompanyId());
		Assert.assertEquals(
			existingFIPSSessionSettings.getUserId(),
			newFIPSSessionSettings.getUserId());
		Assert.assertEquals(
			existingFIPSSessionSettings.getUserName(),
			newFIPSSessionSettings.getUserName());
		Assert.assertEquals(
			Time.getShortTimestamp(existingFIPSSessionSettings.getCreateDate()),
			Time.getShortTimestamp(newFIPSSessionSettings.getCreateDate()));
		Assert.assertEquals(
			Time.getShortTimestamp(
				existingFIPSSessionSettings.getModifiedDate()),
			Time.getShortTimestamp(newFIPSSessionSettings.getModifiedDate()));
		Assert.assertEquals(
			existingFIPSSessionSettings.getAbsoluteLifetimeMinutes(),
			newFIPSSessionSettings.getAbsoluteLifetimeMinutes());
		Assert.assertEquals(
			existingFIPSSessionSettings.getIdleTimeoutMinutes(),
			newFIPSSessionSettings.getIdleTimeoutMinutes());
	}

	@Test
	public void testCountByCompanyId() throws Exception {
		_persistence.countByCompanyId(RandomTestUtil.nextLong());

		_persistence.countByCompanyId(0L);
	}

	@Test
	public void testFindByPrimaryKeyExisting() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		FIPSSessionSettings existingFIPSSessionSettings =
			_persistence.findByPrimaryKey(
				newFIPSSessionSettings.getPrimaryKey());

		Assert.assertEquals(
			existingFIPSSessionSettings, newFIPSSessionSettings);
	}

	@Test(expected = NoSuchSessionSettingsException.class)
	public void testFindByPrimaryKeyMissing() throws Exception {
		long pk = RandomTestUtil.nextLong();

		_persistence.findByPrimaryKey(pk);
	}

	@Test
	public void testFindAll() throws Exception {
		_persistence.findAll(
			QueryUtil.ALL_POS, QueryUtil.ALL_POS, getOrderByComparator());
	}

	protected OrderByComparator<FIPSSessionSettings> getOrderByComparator() {
		return OrderByComparatorFactoryUtil.create(
			"FIPSSessionSettings", "mvccVersion", true, "fipsSessionSettingsId",
			true, "companyId", true, "userId", true, "userName", true,
			"createDate", true, "modifiedDate", true, "absoluteLifetimeMinutes",
			true, "idleTimeoutMinutes", true);
	}

	@Test
	public void testFetchByPrimaryKeyExisting() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		FIPSSessionSettings existingFIPSSessionSettings =
			_persistence.fetchByPrimaryKey(
				newFIPSSessionSettings.getPrimaryKey());

		Assert.assertEquals(
			existingFIPSSessionSettings, newFIPSSessionSettings);
	}

	@Test
	public void testFetchByPrimaryKeyMissing() throws Exception {
		long pk = RandomTestUtil.nextLong();

		FIPSSessionSettings missingFIPSSessionSettings =
			_persistence.fetchByPrimaryKey(pk);

		Assert.assertNull(missingFIPSSessionSettings);
	}

	@Test
	public void testFetchByPrimaryKeysWithMultiplePrimaryKeysWhereAllPrimaryKeysExist()
		throws Exception {

		FIPSSessionSettings newFIPSSessionSettings1 = addFIPSSessionSettings();
		FIPSSessionSettings newFIPSSessionSettings2 = addFIPSSessionSettings();

		Set<Serializable> primaryKeys = new HashSet<Serializable>();

		primaryKeys.add(newFIPSSessionSettings1.getPrimaryKey());
		primaryKeys.add(newFIPSSessionSettings2.getPrimaryKey());

		Map<Serializable, FIPSSessionSettings> fipsSessionSettingses =
			_persistence.fetchByPrimaryKeys(primaryKeys);

		Assert.assertEquals(2, fipsSessionSettingses.size());
		Assert.assertEquals(
			newFIPSSessionSettings1,
			fipsSessionSettingses.get(newFIPSSessionSettings1.getPrimaryKey()));
		Assert.assertEquals(
			newFIPSSessionSettings2,
			fipsSessionSettingses.get(newFIPSSessionSettings2.getPrimaryKey()));
	}

	@Test
	public void testFetchByPrimaryKeysWithMultiplePrimaryKeysWhereNoPrimaryKeysExist()
		throws Exception {

		long pk1 = RandomTestUtil.nextLong();

		long pk2 = RandomTestUtil.nextLong();

		Set<Serializable> primaryKeys = new HashSet<Serializable>();

		primaryKeys.add(pk1);
		primaryKeys.add(pk2);

		Map<Serializable, FIPSSessionSettings> fipsSessionSettingses =
			_persistence.fetchByPrimaryKeys(primaryKeys);

		Assert.assertTrue(fipsSessionSettingses.isEmpty());
	}

	@Test
	public void testFetchByPrimaryKeysWithMultiplePrimaryKeysWhereSomePrimaryKeysExist()
		throws Exception {

		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		long pk = RandomTestUtil.nextLong();

		Set<Serializable> primaryKeys = new HashSet<Serializable>();

		primaryKeys.add(newFIPSSessionSettings.getPrimaryKey());
		primaryKeys.add(pk);

		Map<Serializable, FIPSSessionSettings> fipsSessionSettingses =
			_persistence.fetchByPrimaryKeys(primaryKeys);

		Assert.assertEquals(1, fipsSessionSettingses.size());
		Assert.assertEquals(
			newFIPSSessionSettings,
			fipsSessionSettingses.get(newFIPSSessionSettings.getPrimaryKey()));
	}

	@Test
	public void testFetchByPrimaryKeysWithNoPrimaryKeys() throws Exception {
		Set<Serializable> primaryKeys = new HashSet<Serializable>();

		Map<Serializable, FIPSSessionSettings> fipsSessionSettingses =
			_persistence.fetchByPrimaryKeys(primaryKeys);

		Assert.assertTrue(fipsSessionSettingses.isEmpty());
	}

	@Test
	public void testFetchByPrimaryKeysWithOnePrimaryKey() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		Set<Serializable> primaryKeys = new HashSet<Serializable>();

		primaryKeys.add(newFIPSSessionSettings.getPrimaryKey());

		Map<Serializable, FIPSSessionSettings> fipsSessionSettingses =
			_persistence.fetchByPrimaryKeys(primaryKeys);

		Assert.assertEquals(1, fipsSessionSettingses.size());
		Assert.assertEquals(
			newFIPSSessionSettings,
			fipsSessionSettingses.get(newFIPSSessionSettings.getPrimaryKey()));
	}

	@Test
	public void testActionableDynamicQuery() throws Exception {
		final IntegerWrapper count = new IntegerWrapper();

		ActionableDynamicQuery actionableDynamicQuery =
			FIPSSessionSettingsLocalServiceUtil.getActionableDynamicQuery();

		actionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod
				<FIPSSessionSettings>() {

				@Override
				public void performAction(
					FIPSSessionSettings fipsSessionSettings) {

					Assert.assertNotNull(fipsSessionSettings);

					count.increment();
				}

			});

		actionableDynamicQuery.performActions();

		Assert.assertEquals(count.getValue(), _persistence.countAll());
	}

	@Test
	public void testDynamicQueryByPrimaryKeyExisting() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
			FIPSSessionSettings.class, _dynamicQueryClassLoader);

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq(
				"fipsSessionSettingsId",
				newFIPSSessionSettings.getFipsSessionSettingsId()));

		List<FIPSSessionSettings> result = _persistence.findWithDynamicQuery(
			dynamicQuery);

		Assert.assertEquals(1, result.size());

		FIPSSessionSettings existingFIPSSessionSettings = result.get(0);

		Assert.assertEquals(
			existingFIPSSessionSettings, newFIPSSessionSettings);
	}

	@Test
	public void testDynamicQueryByPrimaryKeyMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
			FIPSSessionSettings.class, _dynamicQueryClassLoader);

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq(
				"fipsSessionSettingsId", RandomTestUtil.nextLong()));

		List<FIPSSessionSettings> result = _persistence.findWithDynamicQuery(
			dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testDynamicQueryByProjectionExisting() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
			FIPSSessionSettings.class, _dynamicQueryClassLoader);

		dynamicQuery.setProjection(
			ProjectionFactoryUtil.property("fipsSessionSettingsId"));

		Object newFipsSessionSettingsId =
			newFIPSSessionSettings.getFipsSessionSettingsId();

		dynamicQuery.add(
			RestrictionsFactoryUtil.in(
				"fipsSessionSettingsId",
				new Object[] {newFipsSessionSettingsId}));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(1, result.size());

		Object existingFipsSessionSettingsId = result.get(0);

		Assert.assertEquals(
			existingFipsSessionSettingsId, newFipsSessionSettingsId);
	}

	@Test
	public void testDynamicQueryByProjectionMissing() throws Exception {
		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
			FIPSSessionSettings.class, _dynamicQueryClassLoader);

		dynamicQuery.setProjection(
			ProjectionFactoryUtil.property("fipsSessionSettingsId"));

		dynamicQuery.add(
			RestrictionsFactoryUtil.in(
				"fipsSessionSettingsId",
				new Object[] {RandomTestUtil.nextLong()}));

		List<Object> result = _persistence.findWithDynamicQuery(dynamicQuery);

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void testResetOriginalValues() throws Exception {
		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		_persistence.clearCache();

		_assertOriginalValues(
			_persistence.findByPrimaryKey(
				newFIPSSessionSettings.getPrimaryKey()));
	}

	@Test
	public void testResetOriginalValuesWithDynamicQueryLoadFromDatabase()
		throws Exception {

		_testResetOriginalValuesWithDynamicQuery(true);
	}

	@Test
	public void testResetOriginalValuesWithDynamicQueryLoadFromSession()
		throws Exception {

		_testResetOriginalValuesWithDynamicQuery(false);
	}

	private void _testResetOriginalValuesWithDynamicQuery(boolean clearSession)
		throws Exception {

		FIPSSessionSettings newFIPSSessionSettings = addFIPSSessionSettings();

		if (clearSession) {
			Session session = _persistence.openSession();

			session.flush();

			session.clear();
		}

		DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
			FIPSSessionSettings.class, _dynamicQueryClassLoader);

		dynamicQuery.add(
			RestrictionsFactoryUtil.eq(
				"fipsSessionSettingsId",
				newFIPSSessionSettings.getFipsSessionSettingsId()));

		List<FIPSSessionSettings> result = _persistence.findWithDynamicQuery(
			dynamicQuery);

		_assertOriginalValues(result.get(0));
	}

	private void _assertOriginalValues(
		FIPSSessionSettings fipsSessionSettings) {

		Assert.assertEquals(
			Long.valueOf(fipsSessionSettings.getCompanyId()),
			ReflectionTestUtil.<Long>invoke(
				fipsSessionSettings, "getColumnOriginalValue",
				new Class<?>[] {String.class}, "companyId"));
	}

	protected FIPSSessionSettings addFIPSSessionSettings() throws Exception {
		long pk = RandomTestUtil.nextLong();

		FIPSSessionSettings fipsSessionSettings = _persistence.create(pk);

		fipsSessionSettings.setCompanyId(RandomTestUtil.nextLong());

		fipsSessionSettings.setUserId(RandomTestUtil.nextLong());

		fipsSessionSettings.setUserName(RandomTestUtil.randomString());

		fipsSessionSettings.setCreateDate(RandomTestUtil.nextDate());

		fipsSessionSettings.setModifiedDate(RandomTestUtil.nextDate());

		fipsSessionSettings.setAbsoluteLifetimeMinutes(
			RandomTestUtil.nextInt());

		fipsSessionSettings.setIdleTimeoutMinutes(RandomTestUtil.nextInt());

		_fipsSessionSettingses.add(_persistence.update(fipsSessionSettings));

		return fipsSessionSettings;
	}

	private List<FIPSSessionSettings> _fipsSessionSettingses =
		new ArrayList<FIPSSessionSettings>();
	private FIPSSessionSettingsPersistence _persistence;
	private ClassLoader _dynamicQueryClassLoader;

}
// LIFERAY-SERVICE-BUILDER-HASH:-1545363534