package com.liferay.captcha.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

@ExtendedObjectClassDefinition(generateUI = false)
@Meta.OCD(
	factory = true,
	id = "com.liferay.captcha.internal.configuration.FunctionCaptchaEngineConfiguration"
)
public interface FunctionCaptchaEngineConfiguration {
	@Meta.AD(name = "key", required = false)
	public String key();

	@Meta.AD(name = "name", required = false)
	public String name();

	@Meta.AD(
		name = "oauth2-application-external-reference-code", required = false,
		type = Meta.Type.String
	)
	public String oAuth2ApplicationExternalReferenceCode();

	@Meta.AD(
		name = "captcha-engine-type-settings", required = false,
		type = Meta.Type.String
	)
	public String CaptchaEngineTypeSettings();

	@Meta.AD
	public String taglibPath();

}
