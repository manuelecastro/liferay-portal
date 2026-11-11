/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.antivirus.async.store.internal.retry;

import com.liferay.antivirus.async.store.AntivirusScannerHelper;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.scheduler.SchedulerJobConfiguration;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.osgi.framework.BundleContext;

/**
 * @author Manuele Castro
 */
public class AntivirusAsyncRetrySchedulerImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		ReflectionTestUtil.setFieldValue(
			_antivirusAsyncRetrySchedulerImpl, "_antivirusScannerHelper",
			_antivirusScannerHelper);

		BundleContext bundleContext = Mockito.mock(BundleContext.class);

		_antivirusAsyncRetrySchedulerImpl.activate(
			bundleContext, Collections.emptyMap());

		ArgumentCaptor<SchedulerJobConfiguration> argumentCaptor =
			ArgumentCaptor.forClass(SchedulerJobConfiguration.class);

		Mockito.verify(
			bundleContext
		).registerService(
			Mockito.eq(SchedulerJobConfiguration.class),
			argumentCaptor.capture(), Mockito.isNull()
		);

		SchedulerJobConfiguration schedulerJobConfiguration =
			argumentCaptor.getValue();

		_unsafeRunnable =
			schedulerJobConfiguration.getJobExecutorUnsafeRunnable();
	}

	@Test
	public void testGetJobExecutorUnsafeRunnable() throws Exception {
		Mockito.doAnswer(
			invocationOnMock -> {
				_antivirusAsyncRetrySchedulerImpl.schedule(
					invocationOnMock.getArgument(0));

				return null;
			}
		).when(
			_antivirusScannerHelper
		).processMessage(
			Mockito.any(Message.class)
		);

		Message message1 = _createMessage(RandomTestUtil.randomString());
		Message message2 = _createMessage(RandomTestUtil.randomString());

		_antivirusAsyncRetrySchedulerImpl.schedule(message1);
		_antivirusAsyncRetrySchedulerImpl.schedule(message2);

		_unsafeRunnable.run();

		Mockito.verify(
			_antivirusScannerHelper
		).processMessage(
			message1
		);

		Mockito.verify(
			_antivirusScannerHelper
		).processMessage(
			message2
		);

		_unsafeRunnable.run();

		Mockito.verify(
			_antivirusScannerHelper, Mockito.times(2)
		).processMessage(
			message1
		);

		Mockito.verify(
			_antivirusScannerHelper, Mockito.times(2)
		).processMessage(
			message2
		);
	}

	@Test
	public void testSchedule() throws Exception {
		String jobName = RandomTestUtil.randomString();

		Message message = _createMessage(jobName);

		_antivirusAsyncRetrySchedulerImpl.schedule(message);

		_antivirusAsyncRetrySchedulerImpl.schedule(_createMessage(jobName));

		_unsafeRunnable.run();

		Mockito.verify(
			_antivirusScannerHelper
		).processMessage(
			message
		);

		Mockito.verifyNoMoreInteractions(_antivirusScannerHelper);
	}

	@Test
	public void testUnschedule() throws Exception {
		Message message = _createMessage(RandomTestUtil.randomString());

		_antivirusAsyncRetrySchedulerImpl.schedule(message);
		_antivirusAsyncRetrySchedulerImpl.unschedule(message);

		_unsafeRunnable.run();

		Mockito.verifyNoInteractions(_antivirusScannerHelper);
	}

	private Message _createMessage(String jobName) {
		Message message = new Message();

		message.put("jobName", jobName);

		return message;
	}

	private final AntivirusAsyncRetrySchedulerImpl
		_antivirusAsyncRetrySchedulerImpl =
			new AntivirusAsyncRetrySchedulerImpl();
	private final AntivirusScannerHelper _antivirusScannerHelper = Mockito.mock(
		AntivirusScannerHelper.class);
	private UnsafeRunnable<Exception> _unsafeRunnable;

}