/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.cell.internal.security.auth.verifier;

import com.liferay.ai.hub.cell.security.JWTTokenUtil;
import com.liferay.portal.kernel.security.auth.AccessControlContext;
import com.liferay.portal.kernel.security.auth.verifier.AuthVerifierResult;
import com.liferay.portal.kernel.security.service.access.policy.ServiceAccessPolicy;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Manuele Castro
 */
public class AIHubCellRequestAuthVerifierTest {

	@ClassRule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testVerify() throws Exception {
		AuthVerifierResult authVerifierResult = _verify(
			RandomTestUtil.randomString());

		Assert.assertEquals(
			AuthVerifierResult.State.INVALID_CREDENTIALS,
			authVerifierResult.getState());

		authVerifierResult = _verify(null);

		Assert.assertEquals(
			AuthVerifierResult.State.NOT_APPLICABLE,
			authVerifierResult.getState());

		long userId = RandomTestUtil.randomLong();

		String token = JWTTokenUtil.generateToken(
			TimeUnit.MINUTES.toMillis(1), RandomTestUtil.randomString(),
			userId);

		authVerifierResult = _verify(token);

		Assert.assertEquals(
			AuthVerifierResult.State.SUCCESS, authVerifierResult.getState());
		Assert.assertEquals(userId, authVerifierResult.getUserId());

		Map<String, Object> settings = authVerifierResult.getSettings();

		@SuppressWarnings("unchecked")
		List<String> serviceAccessPolicyNames = (List<String>)settings.get(
			ServiceAccessPolicy.SERVICE_ACCESS_POLICY_NAMES);

		Assert.assertNotNull(serviceAccessPolicyNames);
		Assert.assertTrue(
			serviceAccessPolicyNames.contains("AI_HUB_CELL_TOKEN"));
	}

	private AuthVerifierResult _verify(String token) throws Exception {
		HttpServletRequest httpServletRequest = Mockito.mock(
			HttpServletRequest.class);

		Mockito.when(
			httpServletRequest.getHeader("Liferay-AI-Hub-Cell-On-Behalf-Of")
		).thenReturn(
			token
		);

		AccessControlContext accessControlContext = new AccessControlContext();

		accessControlContext.setRequest(httpServletRequest);

		AIHubCellRequestAuthVerifier authVerifier =
			new AIHubCellRequestAuthVerifier();

		return authVerifier.verify(accessControlContext, new Properties());
	}

}