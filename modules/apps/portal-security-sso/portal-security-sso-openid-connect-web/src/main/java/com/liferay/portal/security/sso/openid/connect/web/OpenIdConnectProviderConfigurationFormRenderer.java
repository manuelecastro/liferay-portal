package com.liferay.portal.security.sso.openid.connect.web;

import com.liferay.configuration.admin.display.ConfigurationFormRenderer;
import com.liferay.portal.security.sso.openid.connect.configuration.OpenIdConnectProviderConfiguration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.Map;

@Component(service = ConfigurationFormRenderer.class)
public class OpenIdConnectProviderConfigurationFormRenderer implements ConfigurationFormRenderer {
	@Override
	public String getPid() {
		return OpenIdConnectProviderConfiguration.class.getName();
	}

	@Override
	public Map<String, Object> getRequestParameters(
		HttpServletRequest httpServletRequest) {
		return null;
	}

	@Override
	public void render(
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) throws IOException {

		try {
			RequestDispatcher requestDispatcher =
				_servletContext.getRequestDispatcher(
					"/portal_settings/open_id_connect_configuration.jsp");

			requestDispatcher.include(httpServletRequest, httpServletResponse);
		} catch (Exception exception) {
			throw new IOException(
				"Unable to render /cookies_preference_handling_configuration" +
					"/view.jsp",
				exception);
		}
	}
	@Reference(
		target = "(osgi.web.symbolicname=com.liferay.sso.openid.connect.web)"
	)
	private ServletContext _servletContext;
}
