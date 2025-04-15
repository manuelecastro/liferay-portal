/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.sample;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Manuele Castro
 */
@RequestMapping("/captcha/validation")
@RestController
public class CaptchaValidationRestController extends BaseRestController {

	@PostMapping
	public ResponseEntity<String> post(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		log(jwt, _log, json);

		JSONObject jsonObject = new JSONObject(json);

		String captchaResponse = jsonObject.getString("g-recaptcha-response");

		Http.Options options = new Http.Options();

		options.setLocation("https://www.google.com/recaptcha/api/siteverify");

		try {
			options.addPart(
				"secret", "6Le3FhorAAAAAG3Xmjgz0VphYiPpF4Zq75_E-zhg");
		}
		catch (Exception exception) {
			_log.error(exception);
		}

		options.addPart("remoteip", jsonObject.getString("remoteAddress"));
		options.addPart("response", captchaResponse);
		options.setPost(true);

		String content = null;

		try {
			content = HttpUtil.URLtoString(options);
		}
		catch (Exception ioException) {
			_log.error(ioException);
		}

		if (content == null) {
			_log.error("CAPTCHA did not return a result");
		}

		jsonObject = new JSONObject(content);

		String success = jsonObject.getString("success");

		if (StringUtil.equalsIgnoreCase(success, "true")) {
			jsonObject.put("validation", "passed");
		} else {
			jsonObject.put("validation", "failed");
		}

		return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
	}

	private static final Log _log = LogFactory.getLog(
		CaptchaValidationRestController.class);

}