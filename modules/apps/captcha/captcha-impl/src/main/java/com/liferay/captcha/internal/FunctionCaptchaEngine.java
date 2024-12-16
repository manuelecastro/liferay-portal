package com.liferay.captcha.internal;

import com.liferay.captcha.internal.configuration.FunctionCaptchaEngineConfiguration;
import com.liferay.captcha.simplecaptcha.SimpleCaptchaImpl;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.captcha.Captcha;
import com.liferay.portal.kernel.captcha.CaptchaConfigurationException;
import com.liferay.portal.kernel.captcha.CaptchaException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component(
	configurationPid = "com.liferay.captcha.internal.configuration.FunctionCaptchaEngineConfiguration",
	factory = "com.liferay.captcha.internal.FunctionCaptchaEngine",
	service = Captcha.class
)
public class FunctionCaptchaEngine extends SimpleCaptchaImpl {

	@Override
	public String getTaglibPath() {
		return _functionCaptchaEngineConfiguration.taglibPath();
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
		_functionCaptchaEngineConfiguration =
			ConfigurableUtil.createConfigurable(
				FunctionCaptchaEngineConfiguration.class, properties);
	}

	@Override
	protected boolean validateChallenge(HttpServletRequest httpServletRequest)
		throws CaptchaException {

		String content = null;

		try {
			content = HttpUtil.URLtoString((Http.Options) httpServletRequest);
		}
		catch (IOException ioException) {
			_log.error(ioException);

			throw new CaptchaConfigurationException();
		}

		if (content == null) {
			_log.error("CAPTCHA did not return a result");

			throw new CaptchaConfigurationException();
		}

		try {
			JSONObject jsonObject = _jsonFactory.createJSONObject(content);

			String success = jsonObject.getString("success");

			if (StringUtil.equalsIgnoreCase(success, "true")) {
				return true;
			}

			JSONArray jsonArray = jsonObject.getJSONArray("error-codes");

			if ((jsonArray == null) || (jsonArray.length() == 0)) {
				_log.error("CAPTCHA encountered an error");

				throw new CaptchaConfigurationException();
			}

			StringBundler sb = new StringBundler((jsonArray.length() * 2) - 1);

			for (int i = 0; i < jsonArray.length(); i++) {
				sb.append(jsonArray.getString(i));

				if (i < (jsonArray.length() - 1)) {
					sb.append(StringPool.COMMA_AND_SPACE);
				}
			}

			_log.error("CAPTCHA encountered an error: " + sb.toString());

			throw new CaptchaConfigurationException();
		}
		catch (JSONException jsonException) {
			_log.error(
				"CAPTCHA did not return a valid result: " + content,
				jsonException);

			throw new CaptchaConfigurationException();
		}
	}

	@Override
	protected boolean validateChallenge(PortletRequest portletRequest)
		throws CaptchaException {

		return validateChallenge(
			PortalUtil.getHttpServletRequest(portletRequest));
	}

	private static final Log _log = LogFactoryUtil.getLog(FunctionCaptchaEngine.class);

	private FunctionCaptchaEngineConfiguration
		_functionCaptchaEngineConfiguration;
	@Reference
	private JSONFactory _jsonFactory;

}
