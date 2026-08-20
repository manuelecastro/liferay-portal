<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
FIPSSessionConfiguration fipsSessionConfiguration = fipsAdminDisplayContext.getFIPSSessionConfiguration();

String absoluteLifetimeTimeUnit = fipsSessionConfiguration.absoluteLifetimeTimeUnit();
String idleTimeoutTimeUnit = fipsSessionConfiguration.idleTimeoutTimeUnit();
%>

<liferay-ui:error key="<%= ConfigurationModelListenerException.class.getName() %>" message="please-enter-a-timeout-within-the-allowed-range" />

<portlet:actionURL name="/fips_admin/edit_fips_session_configuration" var="editFIPSSessionConfigurationURL" />

<aui:form action="<%= editFIPSSessionConfigurationURL %>" method="post" name="fm">
	<div class="sheet">
		<div class="panel-group panel-group-flush">
			<aui:fieldset label="session-timeouts">
				<div class="row">
					<div class="col-sm-12 form-group">
						<label class="c-mb-1 c-mt-2 font-weight-semi-bold" for="<portlet:namespace />idleTimeout" id="<portlet:namespace />idleTimeoutLabel">
							<liferay-ui:message key="session-idle-timeout" />
						</label>

						<div class="form-group-autofit">
							<div class="form-group-item">
								<input
									aria-describedby="<portlet:namespace />idleTimeoutHelp"
									class="form-control"
									id="<portlet:namespace />idleTimeout"
									max="<%= FIPSTimeUnitUtil.getMaximum(FIPSConstants.SESSION_IDLE_TIMEOUT_MAX_MINUTES, idleTimeoutTimeUnit) %>"
									min="1"
									name="<portlet:namespace />idleTimeout"
									required
									type="number"
									value="<%= fipsSessionConfiguration.idleTimeout() %>"
								/>
							</div>

							<div class="form-group-item">
								<select aria-label="<%= HtmlUtil.escape(LanguageUtil.get(request, "time-unit")) %>" class="form-control" id="<portlet:namespace />idleTimeoutTimeUnit" name="<portlet:namespace />idleTimeoutTimeUnit">
									<option <%= Objects.equals(FIPSConstants.TIME_UNIT_MINUTES, idleTimeoutTimeUnit) ? "selected" : "" %> value="<%= FIPSConstants.TIME_UNIT_MINUTES %>"><liferay-ui:message key="minutes" /></option>
									<option <%= Objects.equals(FIPSConstants.TIME_UNIT_HOURS, idleTimeoutTimeUnit) ? "selected" : "" %> value="<%= FIPSConstants.TIME_UNIT_HOURS %>"><liferay-ui:message key="hours" /></option>
								</select>
							</div>
						</div>

						<div class="c-mb-1 form-feedback-group" id="<portlet:namespace />idleTimeoutHelp">
							<div class="form-text text-weight-normal">
								<liferay-ui:message arguments='<%= new Object[] {FIPSConstants.SESSION_IDLE_TIMEOUT_MAX_HOURS, LanguageUtil.get(request, "hours")} %>' key="the-maximum-is-x-x" translateArguments="<%= false %>" />
							</div>
						</div>
					</div>
				</div>

				<div class="row">
					<div class="col-sm-12 form-group">
						<label class="c-mb-1 c-mt-2 font-weight-semi-bold" for="<portlet:namespace />absoluteLifetime" id="<portlet:namespace />absoluteLifetimeLabel">
							<liferay-ui:message key="session-absolute-lifetime" />
						</label>

						<div class="form-group-autofit">
							<div class="form-group-item">
								<input
									aria-describedby="<portlet:namespace />absoluteLifetimeHelp"
									class="form-control"
									id="<portlet:namespace />absoluteLifetime"
									max="<%= FIPSTimeUnitUtil.getMaximum(FIPSConstants.SESSION_ABSOLUTE_LIFETIME_MAX_MINUTES, absoluteLifetimeTimeUnit) %>"
									min="1"
									name="<portlet:namespace />absoluteLifetime"
									required
									type="number"
									value="<%= fipsSessionConfiguration.absoluteLifetime() %>"
								/>
							</div>

							<div class="form-group-item">
								<select aria-label="<%= HtmlUtil.escape(LanguageUtil.get(request, "time-unit")) %>" class="form-control" id="<portlet:namespace />absoluteLifetimeTimeUnit" name="<portlet:namespace />absoluteLifetimeTimeUnit">
									<option <%= Objects.equals(FIPSConstants.TIME_UNIT_MINUTES, absoluteLifetimeTimeUnit) ? "selected" : "" %> value="<%= FIPSConstants.TIME_UNIT_MINUTES %>"><liferay-ui:message key="minutes" /></option>
									<option <%= Objects.equals(FIPSConstants.TIME_UNIT_HOURS, absoluteLifetimeTimeUnit) ? "selected" : "" %> value="<%= FIPSConstants.TIME_UNIT_HOURS %>"><liferay-ui:message key="hours" /></option>
									<option <%= Objects.equals(FIPSConstants.TIME_UNIT_DAYS, absoluteLifetimeTimeUnit) ? "selected" : "" %> value="<%= FIPSConstants.TIME_UNIT_DAYS %>"><liferay-ui:message key="days" /></option>
								</select>
							</div>
						</div>

						<div class="c-mb-1 form-feedback-group" id="<portlet:namespace />absoluteLifetimeHelp">
							<div class="form-text text-weight-normal">
								<liferay-ui:message arguments='<%= new Object[] {FIPSConstants.SESSION_ABSOLUTE_LIFETIME_MAX_DAYS, LanguageUtil.get(request, "days")} %>' key="the-maximum-is-x-x" translateArguments="<%= false %>" />
							</div>
						</div>
					</div>
				</div>
			</aui:fieldset>

			<aui:button-row>
				<aui:button type="submit" />
			</aui:button-row>
		</div>
	</div>
</aui:form>

<aui:script>
	function <portlet:namespace />setMaximum(inputId, timeUnit, maximumMinutes) {
		var input = document.getElementById(inputId);

		if (timeUnit === '<%= FIPSConstants.TIME_UNIT_DAYS %>') {
			input.setAttribute('max', maximumMinutes / 1440);
		}
		else if (timeUnit === '<%= FIPSConstants.TIME_UNIT_HOURS %>') {
			input.setAttribute('max', maximumMinutes / 60);
		}
		else {
			input.setAttribute('max', maximumMinutes);
		}
	}

	[
		[
			'<portlet:namespace />idleTimeout',
			<%= FIPSConstants.SESSION_IDLE_TIMEOUT_MAX_MINUTES %>,
		],
		[
			'<portlet:namespace />absoluteLifetime',
			<%= FIPSConstants.SESSION_ABSOLUTE_LIFETIME_MAX_MINUTES %>,
		],
	].forEach(function (item) {
		var timeUnitSelect = document.getElementById(item[0] + 'TimeUnit');

		timeUnitSelect.addEventListener('change', function (event) {
			<portlet:namespace />setMaximum(item[0], event.target.value, item[1]);
		});
	});
</aui:script>