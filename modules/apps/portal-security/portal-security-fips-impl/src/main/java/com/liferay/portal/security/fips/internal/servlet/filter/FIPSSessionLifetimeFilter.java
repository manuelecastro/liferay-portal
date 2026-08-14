/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.internal.servlet.filter;

import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.security.fips.configuration.FIPSSessionConfiguration;
import com.liferay.portal.security.fips.constants.FIPSConstants;
import com.liferay.portal.servlet.filters.BasePortalFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Enforces the absolute session lifetime and keeps the idle timeout current.
 *
 * <p>
 * The idle timeout is reapplied on every request rather than only at login, so
 * that a value saved by the Crypto Officer takes effect on sessions that are
 * already open instead of waiting for the next sign in.
 * </p>
 *
 * <p>
 * An expired session is invalidated and the request continues as a guest. That
 * covers browser traffic and headless traffic with one rule: the portal
 * redirects an anonymous page request to the login screen, while the auth
 * verifier refuses an anonymous <code>/o/</code> request. Registering under the
 * portal servlet context reaches both, since <code>/o/</code> is served from
 * within the portal web application.
 * </p>
 *
 * @author Manuele Castro
 */
@Component(
	property = {
		"dispatcher=FORWARD", "dispatcher=REQUEST", "servlet-context-name=",
		"servlet-filter-name=FIPS Session Lifetime Filter", "url-pattern=/*"
	},
	service = Filter.class
)
public class FIPSSessionLifetimeFilter extends BasePortalFilter {

	@Override
	public boolean isFilterEnabled(
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {

		if (!PropsValues.FIPS_ENABLED ||
			(CompanyThreadLocal.getCompanyId() == 0)) {

			return false;
		}

		return true;
	}

	@Override
	protected void processFilter(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws Exception {

		HttpSession httpSession = httpServletRequest.getSession(false);

		if ((httpSession != null) &&
			(_portal.getUserId(httpServletRequest) > 0)) {

			FIPSSessionConfiguration fipsSessionConfiguration =
				_configurationProvider.getCompanyConfiguration(
					FIPSSessionConfiguration.class,
					CompanyThreadLocal.getCompanyId());

			if (_isExpired(fipsSessionConfiguration, httpSession)) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						"Invalidating session " + httpSession.getId() +
							" because it reached its absolute lifetime");
				}

				httpSession.invalidate();
			}
			else {
				httpSession.setMaxInactiveInterval(
					fipsSessionConfiguration.idleTimeoutMinutes() * 60);
			}
		}

		super.processFilter(
			httpServletRequest, httpServletResponse, filterChain);
	}

	private boolean _isExpired(
		FIPSSessionConfiguration fipsSessionConfiguration,
		HttpSession httpSession) {

		long deadline = GetterUtil.getLong(
			httpSession.getAttribute(FIPSConstants.SESSION_ABSOLUTE_DEADLINE));

		if (deadline <= 0) {
			long absoluteLifetime =
				fipsSessionConfiguration.absoluteLifetimeMinutes() *
					Time.MINUTE;

			deadline = httpSession.getCreationTime() + absoluteLifetime;
		}

		if (System.currentTimeMillis() >= deadline) {
			return true;
		}

		return false;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FIPSSessionLifetimeFilter.class);

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private Portal _portal;

}