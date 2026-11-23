/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.key.internal.secret;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author Manuele Castro
 */
public class SecretIdentifierUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetSecretIdentifier() {
		long companyId = RandomTestUtil.randomLong();
		String key = RandomTestUtil.randomString();
		String pid = RandomTestUtil.randomString();

		_testGetSecretIdentifier(
			StringBundler.concat(
				"config/", pid, "/company/", companyId, StringPool.SLASH, key),
			key, pid + ".scoped~" + RandomTestUtil.randomString(),
			HashMapBuilder.<String, Object>put(
				ConfigurationAdmin.SERVICE_FACTORYPID, pid + ".scoped"
			).put(
				"companyId", companyId
			).build());

		String name = RandomTestUtil.randomString();

		_testGetSecretIdentifier(
			StringBundler.concat(
				"config/", pid, StringPool.SLASH, name, "/company/", companyId,
				StringPool.SLASH, key),
			key, pid + "~" + name,
			HashMapBuilder.<String, Object>put(
				ConfigurationAdmin.SERVICE_FACTORYPID, pid
			).put(
				"companyId", companyId
			).build());

		long groupId = RandomTestUtil.randomLong();

		_testGetSecretIdentifier(
			StringBundler.concat(
				"config/", pid, "/group/", groupId, StringPool.SLASH, key),
			key, pid + ".scoped~" + RandomTestUtil.randomString(),
			HashMapBuilder.<String, Object>put(
				ConfigurationAdmin.SERVICE_FACTORYPID, pid + ".scoped"
			).put(
				"companyId", companyId
			).put(
				"groupId", groupId
			).build());

		String portletInstanceId = RandomTestUtil.randomString();

		_testGetSecretIdentifier(
			StringBundler.concat(
				"config/", pid, "/portlet-instance/", portletInstanceId,
				StringPool.SLASH, key),
			key, pid + ".scoped~" + RandomTestUtil.randomString(),
			HashMapBuilder.<String, Object>put(
				ConfigurationAdmin.SERVICE_FACTORYPID, pid + ".scoped"
			).put(
				"portletInstanceId", portletInstanceId
			).build());

		_testGetSecretIdentifier(
			StringBundler.concat("config/", pid, "/system/0/", key), key, pid,
			Collections.emptyMap());
	}

	private void _testGetSecretIdentifier(
		String expectedSecretIdentifier, String key, String pid,
		Map<String, Object> properties) {

		Assert.assertEquals(
			expectedSecretIdentifier,
			SecretIdentifierUtil.getSecretIdentifier(
				key, pid, properties::get));
	}

}