package com.liferay.portal.security.sso.openid.connect.web.internal.display.context;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.security.sso.openid.connect.configuration.ConfigurationProvider;
import com.liferay.portal.security.sso.openid.connect.configuration.OpenIdConnectProviderConfiguration;

import javax.servlet.http.HttpServletRequest;

public class OpenIdConnectProviderConfigurationDisplayContext {

	public OpenIdConnectProviderConfigurationDisplayContext(
		ConfigurationProvider<OpenIdConnectProviderConfiguration> openIdConnectProviderConfigurationProvider,
		HttpServletRequest httpServletRequest,
		String clientId) {
		_openIdConnectProviderConfigurationProvider = openIdConnectProviderConfigurationProvider;
		_httpServletRequest = httpServletRequest;
		_clientId = clientId;
	}

	public String getProviderName() {
		return _openIdConnectProviderConfigurationProvider.getConfiguration(_clientId).providerName();
	}

	public String getScopes() {
		return _openIdConnectProviderConfigurationProvider.getConfiguration(_clientId).scopes();
	}



	private final ConfigurationProvider<OpenIdConnectProviderConfiguration> _openIdConnectProviderConfigurationProvider;

	private final HttpServletRequest _httpServletRequest;

	private final LiferayPortletResponse _liferayPortletResponse;

	private final ExtendedObjectClassDefinition.Scope _scope;

	private final long _scopePK;

	private final String _clientId;


}
