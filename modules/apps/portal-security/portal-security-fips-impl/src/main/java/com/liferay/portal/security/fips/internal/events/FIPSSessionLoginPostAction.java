/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.internal.events;

import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.security.fips.configuration.FIPSSessionConfiguration;
import com.liferay.portal.security.fips.constants.FIPSConstants;
import com.liferay.portal.security.fips.util.FIPSTimeUnitUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Anchors the absolute session lifetime to the moment of authentication.
 *
 * <p>
 * Runs on <code>login.events.post</code> rather than
 * <code>servlet.session.create.events</code> for two reasons. The company and
 * the user are already resolved, and the event is processed after {@link
 * com.liferay.portal.security.auth.session.AuthenticatedSessionManagerUtil}
 * has renewed the session, so the deadline is not discarded by the
 * invalidate-and-recreate that phishing protection performs.
 * </p>
 *
 * @author Manuele Castro
 */
@Component(property = "key=login.events.post", service = LifecycleAction.class)
public class FIPSSessionLoginPostAction extends Action {

	@Override
	public void run(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws ActionException {

		if (!PropsValues.FIPS_ENABLED) {
			return;
		}

		try {
			FIPSSessionConfiguration fipsSessionConfiguration =
				_configurationProvider.getCompanyConfiguration(
					FIPSSessionConfiguration.class,
					_portal.getCompanyId(httpServletRequest));

			HttpSession httpSession = httpServletRequest.getSession();

			long absoluteLifetimeMinutes = FIPSTimeUnitUtil.toMinutes(
				fipsSessionConfiguration.absoluteLifetime(),
				fipsSessionConfiguration.absoluteLifetimeTimeUnit());

			long absoluteLifetime = absoluteLifetimeMinutes * Time.MINUTE;

			httpSession.setAttribute(
				FIPSConstants.SESSION_ABSOLUTE_DEADLINE,
				System.currentTimeMillis() + absoluteLifetime);

			long idleTimeoutMinutes = FIPSTimeUnitUtil.toMinutes(
				fipsSessionConfiguration.idleTimeout(),
				fipsSessionConfiguration.idleTimeoutTimeUnit());

			httpSession.setMaxInactiveInterval((int)idleTimeoutMinutes * 60);
		}
		catch (Exception exception) {
			throw new ActionException(exception);
		}
	}

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private Portal _portal;

}