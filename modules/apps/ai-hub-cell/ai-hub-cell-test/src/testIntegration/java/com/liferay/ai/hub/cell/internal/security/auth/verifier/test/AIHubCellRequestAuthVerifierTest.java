/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ai.hub.cell.internal.security.auth.verifier.test;

import com.liferay.ai.hub.cell.configuration.AIHubCellConfiguration;
import com.liferay.ai.hub.cell.security.JWTTokenUtil;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.oauth2.provider.constants.ClientProfile;
import com.liferay.oauth2.provider.constants.GrantType;
import com.liferay.oauth2.provider.model.OAuth2Application;
import com.liferay.oauth2.provider.service.OAuth2ApplicationLocalService;
import com.liferay.oauth2.provider.util.OAuth2SecureRandomGenerator;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.test.rule.FeatureFlag;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuele Castro
 */
@FeatureFlag("LPD-62272")
@RunWith(Arquillian.class)
public class AIHubCellRequestAuthVerifierTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@After
	public void tearDown() {
		try {
			ConfigurationTestUtil.deleteConfiguration(
				AIHubCellConfiguration.class.getName());
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	@Test
	public void testVerify() throws Exception {
		Company company = _companyLocalService.getCompany(
			TestPropsValues.getCompanyId());

		String baseURL = StringBundler.concat(
			Http.HTTP_WITH_SLASH, company.getVirtualHostname(), ":8080");

		User user = TestPropsValues.getUser();

		OAuth2Application oAuth2Application =
			_oAuth2ApplicationLocalService.addOAuth2Application(
				user.getCompanyId(), user.getUserId(), user.getFullName(),
				List.of(GrantType.CLIENT_CREDENTIALS), "client_secret_post",
				user.getUserId(),
				OAuth2SecureRandomGenerator.generateClientId(),
				ClientProfile.WEB_APPLICATION.id(),
				OAuth2SecureRandomGenerator.generateClientSecret(), "",
				List.of(), baseURL, 0, null, "AI Hub", "", List.of(baseURL),
				false, Arrays.asList("Liferay.AI.Hub.REST.everything"), false,
				new ServiceContext());

		ConfigurationTestUtil.saveConfiguration(
			AIHubCellConfiguration.class.getName(),
			HashMapDictionaryBuilder.<String, Object>put(
				"clientId", oAuth2Application.getClientId()
			).put(
				"clientSecret", oAuth2Application.getClientSecret()
			).put(
				"serviceURL", baseURL
			).build());

		Assert.assertEquals(
			200,
			_getResponseCode(
				new URL(baseURL + "/o/ai-hub-cell/v1.0/authorization-tokens"),
				"POST"));
		Assert.assertEquals(
			200,
			_getResponseCode(
				new URL(baseURL + "/o/search/v1.0/search?search=test"), "GET"));
		Assert.assertEquals(
			403,
			_getResponseCode(
				new URL(baseURL + "/api/jsonws/portal/get-version"), "GET"));
	}

	private int _getResponseCode(URL url, String method) throws Exception {
		HttpURLConnection httpURLConnection =
			(HttpURLConnection)url.openConnection();

		httpURLConnection.setRequestMethod(method);
		httpURLConnection.setRequestProperty(
			"Liferay-AI-Hub-Cell-On-Behalf-Of",
			JWTTokenUtil.generateToken(
				TimeUnit.MINUTES.toMillis(1), RandomTestUtil.randomString(),
				TestPropsValues.getUserId()));

		return httpURLConnection.getResponseCode();
	}

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private OAuth2ApplicationLocalService _oAuth2ApplicationLocalService;

}