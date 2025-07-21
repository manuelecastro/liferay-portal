/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.sso.openid.connect.internal.configuration;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.security.sso.openid.connect.configuration.ConfigurationProvider;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.liferay.portal.security.sso.openid.connect.configuration.OpenIdConnectProviderConfiguration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.annotations.Component;

/**
 * @author Christian Moura
 */
@Component(
	property = "factoryPid=com.liferay.portal.security.sso.openid.connect.configuration.OpenIdConnectProviderConfiguration",
	service = ConfigurationProvider.class
)
public class OpenIdConnectProviderConfigurationProviderImpl
	implements ConfigurationProvider<OpenIdConnectProviderConfiguration> {

	@Override
	public OpenIdConnectProviderConfiguration getConfiguration(
		String clientId) {

		return _configurations.get(clientId);
	}

	@Override
	public void registerConfiguration(Configuration configuration) {
		Dictionary<String, Object> properties = configuration.getProperties();

		if (properties == null) {
			properties = new HashMapDictionary<>();
		}

		OpenIdConnectProviderConfiguration openIdConnectProviderConfiguration =
			ConfigurableUtil.createConfigurable(
				OpenIdConnectProviderConfiguration.class, properties);

		synchronized (_configurations) {
			_configurations.put(
				GetterUtil.getString(properties.get("openIdConnectClientId")),
				openIdConnectProviderConfiguration);
		}
	}

	private final Map<String, OpenIdConnectProviderConfiguration>
		_configurations = new ConcurrentHashMap<>();

}