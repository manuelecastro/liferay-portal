/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.sample;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

		// JSONObject body = new JSONObject()
		// 	.put("secret", "6Le3FhorAAAAAG3Xmjgz0VphYiPpF4Zq75_E")
		// 	.put("remoteip", jsonObject.getString("remoteAddress"))
		// 	.put("response", jsonObject.getString("captchaResponse"));

		String postResult = 
				postToUrlWithParams(
					"https://www.google.com/recaptcha/api/siteverify", 
					"6Le3FhorAAAAAG3Xmjgz0VphYiPpF4Zq75_E-zhg",
					jsonObject.getString("remoteAddress"), 
					jsonObject.getString("captchaResponse")).getBody();

		jsonObject = new JSONObject(postResult);
		
		Boolean success = jsonObject.getBoolean("success");

		if (success) {
			jsonObject.put("success", "true");
		} else {
			jsonObject.put("success", "false");

			JSONArray errorCodes = jsonObject.getJSONArray("error-codes");

			jsonObject.put("error-codes", errorCodes);
		}

		return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
	}

	private static final Log _log = LogFactory.getLog(
		CaptchaValidationRestController.class);

	private final RestTemplate restTemplate;

    public CaptchaValidationRestController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

	public ResponseEntity<String> postToUrlWithParams(String url, String secret, String remoteip, String response) {
		// Prepare headers
		HttpHeaders httpHeaders = new HttpHeaders();

		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // For x-www-form-urlencoded

		// Prepare request body as form parameters
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("secret", secret);
		map.add("remoteip", remoteip);
		map.add("response", response);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, httpHeaders);

		// Make the POST request
		return restTemplate.postForEntity(url, request, String.class);
	}

}