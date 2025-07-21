<%--
/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<aui:form>

<aui:input label="provider-name" name="providerName" helpMessage="provider-name-help"  required="<%= true %>" type="text" />

<aui:input label="scopes" name="scopes" helpMessage="scopes-help" required="<%= true %>" type="text" />

<aui:input label="discovery-endpoint" helpMessage="discovery-endpoint-help" name="discoveryEndpoint"  type="text" />

<aui:input label="discovery-endpoint-cache-in-millis" helpMessage="discovery-endpoint-cache-in-millis-help" name="discoveryEndpointCacheInMillis"  type="number" />

<aui:input label="authorization-endpoint" helpMessage="authorization-endpoint-help" name="authorizationEndpoint"  type="text" />

<aui:input label="issuer-url" helpMessage="issuer-url-help" name="issuerUrl"  type="text" />

<aui:input label="jwks-uri" helpMessage="jwks-uri-help" name="jwksUri"  type="text" />

<aui:input deflt="RS256" label="id-token-signing-alg-values" helpMessage="id-token-signing-alg-values-help" name="idTokenSigningAlgValues"  type="text" />

<aui:input label="token-endpoint" helpMessage="token-endpoint-help" name="tokenEndpoint"  type="text" />

<aui:input label="token-connection-timeout" helpMessage="token-connection-timeout-help" name="tokenConnectionTimeout"  type="text" />

<aui:input label="user-info-endpoint" helpMessage="user-info-endpoint-help" name="userInfoEndpoint"  type="text" />

<aui:input label="open-id-connect-client-id" helpMessage="open-id-connect-client-id-help" name="openIdConnectClientId"  type="text" />

<aui:input label="open-id-connect-client-secret" helpMessage="open-id-connect-client-secret-help" name="openIdConnectClientSecret"  type="text" />

<aui:input label="registered-id-token-signing-alg"  helpMessage="registered-id-token-signing-alg-help"  name="openIdConnectClientSecret"  type="text" />

<aui:field-wrapper label="custom-claims">
<div id="userAttributeMappingsContentBox">

<%
for (int i = 0; i < 3; i++) {
%>

<div class="form-group-autofit lfr-form-row user-attribute-mapping-row" data-prefix="">
	<div class="form-group-item">
		<aui:select fieldParam="" id="" inlineField="" label="user-field-expression" name="" showEmptyOption="<%= true %>">

			<%
			for (int j = 0; j < 3; j++) {
			%>

				<aui:option data-authsupported="" label="test" selected="" value="test"></aui:option>

			<%
			}
			%>

		</aui:select>
	</div>

	<div class="form-group-item">
		<aui:input cssClass="saml-attribute-field" fieldParam="" id="" inlineField="" label="saml-attribute" name="" type="text" value="" />
	</div>

	<div class="form-group-item form-group-item-label-spacer form-group-item-shrink">
		<aui:input checked='false' cssClass="primary-ctrl" disabled="false" id='test' inlineField="<%= true %>" label="use-to-match-users" name="attribute:userIdentifierExpressionIndex" type="radio" value="" />
	</div>

</div>


<%
}
%>

<aui:input name='test name' type="hidden" value="test value" />

</div>

<aui:script use="liferay-auto-fields">
	new Liferay.AutoFields({
		contentBox: '#userAttributeMappingsContentBox',
		fieldIndexes:
			'<portlet:namespace />userAttributeMappingsIndexes',
		namespace: '<portlet:namespace />',
	}).render();
</aui:script>

</aui:field-wrapper>

</aui:form>