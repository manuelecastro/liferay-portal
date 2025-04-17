/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.captcha.internal.function.captcha;

import com.liferay.captcha.internal.configuration.FunctionCaptchaImplConfiguration;
import com.liferay.captcha.simplecaptcha.SimpleCaptchaImpl;
import com.liferay.osgi.util.configuration.ConfigurationFactoryUtil;
import com.liferay.portal.catapult.PortalCatapult;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.captcha.Captcha;
import com.liferay.portal.kernel.captcha.CaptchaConfigurationException;
import com.liferay.portal.kernel.captcha.CaptchaException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Map;

import javax.portlet.PortletRequest;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Pedro Victor Silvestre
 */
@Component(
	configurationPid = "com.liferay.captcha.internal.configuration.FunctionCaptchaImplConfiguration",
	factory = "com.liferay.captcha.internal.function.captcha.FunctionCaptchaImpl",
	service = Captcha.class
)
public class FunctionCaptchaImpl extends SimpleCaptchaImpl {

	@Override
	public String getTaglibPath() {
		return _TAGLIB_PATH;
	}

	@Override
	public String getName() {
		return _functionCaptchaImplConfiguration.captchaName();
	}

	@Override
	public void serveImage(
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void serveImage(
		ResourceRequest resourceRequest, ResourceResponse resourceResponse) {

		throw new UnsupportedOperationException();
	}

	@Activate
	protected void activate(Map<String, Object> properties) {
		_companyId = ConfigurationFactoryUtil.getCompanyId(
			_companyLocalService, properties);

		_functionCaptchaImplConfiguration = ConfigurableUtil.createConfigurable(
			FunctionCaptchaImplConfiguration.class, properties);
	}

	@Override
	protected boolean validateChallenge(HttpServletRequest httpServletRequest)
		throws CaptchaException {

		String captchaResponse = ParamUtil.getString(
			httpServletRequest,
			_functionCaptchaImplConfiguration.captchaClientResponseParameter());

		while (Validator.isBlank(captchaResponse) &&
			   (httpServletRequest instanceof
				   HttpServletRequestWrapper httpServletRequestWrapper)) {

			httpServletRequest =
				(HttpServletRequest)httpServletRequestWrapper.getRequest();

			captchaResponse = ParamUtil.getString(
				httpServletRequest,
				_functionCaptchaImplConfiguration.
					captchaClientResponseParameter());
		}

		if (Validator.isBlank(captchaResponse)) {
			_log.error(
				"CAPTCHA challenge is null. User " +
					httpServletRequest.getRemoteUser() +
						" may be trying to circumvent the CAPTCHA.");

			throw new CaptchaException();
		}

		JSONObject payloadJSONObject = _jsonFactory.createJSONObject();

		payloadJSONObject.put("captchaResponse", captchaResponse);
		payloadJSONObject.put("remoteAddress",
			httpServletRequest.getRemoteAddr());

		try {
			User user = _userLocalService.getUserByScreenName(
				_companyId, "default-service-account");

			JSONObject jsonObject = _jsonFactory.createJSONObject(
				new String(
					_portalCatapult.launch(
						_companyId, Http.Method.POST,
						_functionCaptchaImplConfiguration.
							oAuth2ApplicationExternalReferenceCode(),
						payloadJSONObject,
						_functionCaptchaImplConfiguration.resourcePath(),
						user.getUserId()
					).get()));

			String validation = jsonObject.getString("validation");

			if (StringUtil.equalsIgnoreCase(validation, "passed")) {
				return true;
			}

			_log.error(
				"Challenge validation failed " +
					_functionCaptchaImplConfiguration.captchaName() +
						" encountered an error.");

			throw new CaptchaConfigurationException();
		}
		catch (Exception exception) {
			_log.error(
				_functionCaptchaImplConfiguration.captchaName() +
					" did not return a valid result",
				exception);

			throw new CaptchaConfigurationException();
		}
	}

	@Override
	protected boolean validateChallenge(PortletRequest portletRequest)
		throws CaptchaException {

		return validateChallenge(
			PortalUtil.getHttpServletRequest(portletRequest));
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FunctionCaptchaImpl.class);

	private long _companyId;

	@Reference
	private CompanyLocalService _companyLocalService;

	private volatile FunctionCaptchaImplConfiguration
		_functionCaptchaImplConfiguration;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private PortalCatapult _portalCatapult;

	@Reference
	private UserLocalService _userLocalService;

	private static final String _TAGLIB_PATH = "/captcha/recaptcha.jsp";

}