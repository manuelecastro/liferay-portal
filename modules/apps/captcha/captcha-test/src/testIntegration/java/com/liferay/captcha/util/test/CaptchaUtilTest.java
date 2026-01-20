/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.captcha.util.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.captcha.configuration.CaptchaConfiguration;
import com.liferay.captcha.util.CaptchaUtil;
import com.liferay.portal.configuration.test.util.CompanyConfigurationTemporarySwapper;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class CaptchaUtilTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testCheck() throws Exception {
		try (CompanyConfigurationTemporarySwapper
				companyConfigurationTemporarySwapper =
					new CompanyConfigurationTemporarySwapper(
						TestPropsValues.getCompanyId(),
						CaptchaConfiguration.class.getName(),
						new HashMapDictionaryBuilder(
						).<String, Object>put(
							"maxChallenges", 0
						).build())) {

			MockHttpSession mockHttpSession = new MockHttpSession(
				new MockServletContext(), RandomTestUtil.randomString());

			String captchaId1 = RandomTestUtil.randomString();

			MockHttpServletRequest mockHttpServletRequest1 =
				_getMockHttpServletRequest(mockHttpSession, captchaId1);

			CaptchaUtil.serveImage(
				mockHttpServletRequest1, new MockHttpServletResponse());

			String captchaId2 = RandomTestUtil.randomString();

			CaptchaUtil.serveImage(
				_getMockHttpServletRequest(mockHttpSession, captchaId2),
				new MockHttpServletResponse());

			String captchaText = (String)mockHttpSession.getAttribute(
				captchaId1 + WebKeys.CAPTCHA_TEXT);

			Assert.assertNotNull(captchaText);

			Assert.assertNotNull(
				mockHttpSession.getAttribute(
					captchaId2 + WebKeys.CAPTCHA_TEXT));

			MockHttpServletRequest mockHttpServletRequest2 =
				_getMockHttpServletRequest(mockHttpSession, captchaId1);

			mockHttpServletRequest2.setParameter("captchaText", captchaText);

			CaptchaUtil.check(mockHttpServletRequest2);
		}
	}

	private MockHttpServletRequest _getMockHttpServletRequest(
		MockHttpSession mockHttpSession, String captchaId) {

		MockHttpServletRequest mockHttpServletRequest =
			new MockHttpServletRequest();

		mockHttpServletRequest.setParameter("captchaId", captchaId);
		mockHttpServletRequest.setSession(mockHttpSession);

		return mockHttpServletRequest;
	}

}