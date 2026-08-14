<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
FIPSSessionConfiguration fipsSessionConfiguration = (FIPSSessionConfiguration)request.getAttribute(FIPSSessionConfiguration.class.getName());
%>

<liferay-ui:error key="<%= ConfigurationModelListenerException.class.getName() %>" message="please-enter-a-timeout-within-the-allowed-range" />

<portlet:actionURL name="/fips_admin/edit_fips_session_configuration" var="editFIPSSessionConfigurationURL" />

<aui:form action="<%= editFIPSSessionConfigurationURL %>" method="post" name="fm">
	<aui:fieldset markupView="lexicon">
		<aui:input helpMessage='<%= LanguageUtil.format(request, "set-a-value-in-minutes-between-x-and-x", new Object[] {1, FIPSConstants.SESSION_IDLE_TIMEOUT_MAX_MINUTES}, false) %>' label="session-idle-timeout" max="<%= FIPSConstants.SESSION_IDLE_TIMEOUT_MAX_MINUTES %>" min="1" name="idleTimeoutMinutes" required="<%= true %>" type="number" value="<%= fipsSessionConfiguration.idleTimeoutMinutes() %>" />

		<aui:input helpMessage='<%= LanguageUtil.format(request, "set-a-value-in-minutes-between-x-and-x", new Object[] {1, FIPSConstants.SESSION_ABSOLUTE_LIFETIME_MAX_MINUTES}, false) %>' label="session-absolute-lifetime" max="<%= FIPSConstants.SESSION_ABSOLUTE_LIFETIME_MAX_MINUTES %>" min="1" name="absoluteLifetimeMinutes" required="<%= true %>" type="number" value="<%= fipsSessionConfiguration.absoluteLifetimeMinutes() %>" />
	</aui:fieldset>

	<aui:button-row>
		<aui:button type="submit" />
	</aui:button-row>
</aui:form>