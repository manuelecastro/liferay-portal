/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.antivirus.async.store.internal.retry.test;

import com.liferay.antivirus.async.store.configuration.AntivirusAsyncConfiguration;
import com.liferay.antivirus.async.store.retry.AntivirusAsyncRetryScheduler;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.antivirus.AntivirusScanner;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.test.util.ConfigurationTemporarySwapper;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.StorageType;
import com.liferay.portal.kernel.scheduler.messaging.SchedulerResponse;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class AntivirusAsyncRetrySchedulerImplTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testSchedule() throws Exception {
		ServiceRegistration<AntivirusScanner> serviceRegistration =
			_bundleContext.registerService(
				AntivirusScanner.class,
				(AntivirusScanner)ProxyUtil.newProxyInstance(
					AntivirusScanner.class.getClassLoader(),
					new Class<?>[] {AntivirusScanner.class},
					(proxy, method, args) -> method.getDefaultValue()),
				null);

		try (ConfigurationTemporarySwapper configurationTemporarySwapper =
				new ConfigurationTemporarySwapper(
					AntivirusAsyncConfiguration.class.getName(),
					HashMapDictionaryBuilder.<String, Object>put(
						"retryCronExpression", "0 0 0 1 1 ? 2099"
					).build())) {

			AntivirusAsyncRetryScheduler antivirusAsyncRetryScheduler =
				_getService(
					AntivirusAsyncRetryScheduler.class.getName(),
					"com.liferay.antivirus.async.store.internal.retry." +
						"AntivirusAsyncRetrySchedulerImpl");

			List<SchedulerResponse> schedulerResponses1 =
				_schedulerEngineHelper.getScheduledJobs(
					StorageType.MEMORY_CLUSTERED);

			for (int i = 0; i < 10; i++) {
				Message message = new Message();

				message.put("jobName", RandomTestUtil.randomString());

				antivirusAsyncRetryScheduler.schedule(message);
			}

			List<SchedulerResponse> schedulerResponses2 =
				_schedulerEngineHelper.getScheduledJobs(
					StorageType.MEMORY_CLUSTERED);

			Assert.assertEquals(
				schedulerResponses2.toString(), schedulerResponses1.size(),
				schedulerResponses2.size());
		}
		finally {
			serviceRegistration.unregister();
		}
	}

	private <T> T _getService(String className, String componentName)
		throws Exception {

		String filterString = "(component.name=" + componentName + ")";

		ServiceReference<T>[] serviceReferences = null;

		int waitTime = 0;

		while (ArrayUtil.isEmpty(serviceReferences)) {
			serviceReferences =
				(ServiceReference<T>[])_bundleContext.getServiceReferences(
					className, filterString);

			if (waitTime >= TestPropsValues.CI_TEST_TIMEOUT_TIME) {
				throw new IllegalStateException(
					StringBundler.concat(
						"Timed out while waiting for service ", className, " ",
						filterString));
			}

			if (serviceReferences != null) {
				continue;
			}

			Thread.sleep(_SLEEP_TIME);

			waitTime += _SLEEP_TIME;
		}

		return _bundleContext.getService(serviceReferences[0]);
	}

	private static final int _SLEEP_TIME = 2000;

	private static final BundleContext _bundleContext =
		SystemBundleUtil.getBundleContext();

	@Inject
	private SchedulerEngineHelper _schedulerEngineHelper;

}