/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.model;

import com.liferay.portal.kernel.annotation.ImplementationClassName;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.util.Accessor;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The extended model interface for the FIPSSessionSettings service. Represents a row in the &quot;FIPSSessionSettings&quot; database table, with each column mapped to a property of this class.
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettingsModel
 * @generated
 */
@ImplementationClassName(
	"com.liferay.portal.security.fips.model.impl.FIPSSessionSettingsImpl"
)
@ProviderType
public interface FIPSSessionSettings
	extends FIPSSessionSettingsModel, PersistedModel {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this interface directly. Add methods to <code>com.liferay.portal.security.fips.model.impl.FIPSSessionSettingsImpl</code> and rerun ServiceBuilder to automatically copy the method declarations to this interface.
	 */
	public static final Accessor<FIPSSessionSettings, Long>
		FIPS_SESSION_SETTINGS_ID_ACCESSOR =
			new Accessor<FIPSSessionSettings, Long>() {

				@Override
				public Long get(FIPSSessionSettings fipsSessionSettings) {
					return fipsSessionSettings.getFipsSessionSettingsId();
				}

				@Override
				public Class<Long> getAttributeClass() {
					return Long.class;
				}

				@Override
				public Class<FIPSSessionSettings> getTypeClass() {
					return FIPSSessionSettings.class;
				}

			};

}
// LIFERAY-SERVICE-BUILDER-HASH:1676475895