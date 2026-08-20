/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.web.internal.display.context;

import com.liferay.frontend.taglib.clay.servlet.taglib.util.NavigationItem;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.NavigationItemList;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.security.fips.configuration.FIPSSessionConfiguration;

import jakarta.portlet.RenderRequest;
import jakarta.portlet.RenderResponse;

import java.util.List;

/**
 * Drives the FIPS Admin page. The page is laid out as a navigation bar over a
 * single tab today because the consolidated dashboard in LPD-93619 adds
 * further tabs beside it. A new tab is a name in <code>_TABS1_NAMES</code>, a
 * language key of the same name, and a JSP of the same name; the first entry
 * is the default tab.
 *
 * @author Manuele Castro
 */
public class FIPSAdminDisplayContext {

	public FIPSAdminDisplayContext(
		FIPSSessionConfiguration fipsSessionConfiguration,
		RenderRequest renderRequest, RenderResponse renderResponse) {

		_fipsSessionConfiguration = fipsSessionConfiguration;
		_renderRequest = renderRequest;
		_renderResponse = renderResponse;
	}

	public FIPSSessionConfiguration getFIPSSessionConfiguration() {
		return _fipsSessionConfiguration;
	}

	public List<NavigationItem> getNavigationItems() {
		String tabs1 = getTabs1();

		ThemeDisplay themeDisplay = (ThemeDisplay)_renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		return new NavigationItemList() {
			{
				for (String tabs1Name : _TABS1_NAMES) {
					add(
						navigationItem -> {
							navigationItem.setActive(tabs1.equals(tabs1Name));
							navigationItem.setHref(
								_renderResponse.createRenderURL(), "tabs1",
								tabs1Name);
							navigationItem.setLabel(
								LanguageUtil.get(
									themeDisplay.getLocale(), tabs1Name));
						});
				}
			}
		};
	}

	public String getTabs1() {
		return ParamUtil.getString(_renderRequest, "tabs1", _TABS1_NAMES[0]);
	}

	private static final String[] _TABS1_NAMES = {"session"};

	private final FIPSSessionConfiguration _fipsSessionConfiguration;
	private final RenderRequest _renderRequest;
	private final RenderResponse _renderResponse;

}