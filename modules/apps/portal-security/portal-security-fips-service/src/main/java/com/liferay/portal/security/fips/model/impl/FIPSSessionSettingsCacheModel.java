/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.model.impl;

import com.liferay.petra.lang.HashUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.CacheModel;
import com.liferay.portal.kernel.model.MVCCModel;
import com.liferay.portal.security.fips.model.FIPSSessionSettings;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Date;

/**
 * The cache model class for representing FIPSSessionSettings in entity cache.
 *
 * @author Brian Wing Shun Chan
 * @generated
 */
public class FIPSSessionSettingsCacheModel
	implements CacheModel<FIPSSessionSettings>, Externalizable, MVCCModel {

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof FIPSSessionSettingsCacheModel)) {
			return false;
		}

		FIPSSessionSettingsCacheModel fipsSessionSettingsCacheModel =
			(FIPSSessionSettingsCacheModel)object;

		if ((fipsSessionSettingsId ==
				fipsSessionSettingsCacheModel.fipsSessionSettingsId) &&
			(mvccVersion == fipsSessionSettingsCacheModel.mvccVersion)) {

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = HashUtil.hash(0, fipsSessionSettingsId);

		return HashUtil.hash(hashCode, mvccVersion);
	}

	@Override
	public long getMvccVersion() {
		return mvccVersion;
	}

	@Override
	public void setMvccVersion(long mvccVersion) {
		this.mvccVersion = mvccVersion;
	}

	@Override
	public String toString() {
		StringBundler sb = new StringBundler(19);

		sb.append("{mvccVersion=");
		sb.append(mvccVersion);
		sb.append(", fipsSessionSettingsId=");
		sb.append(fipsSessionSettingsId);
		sb.append(", companyId=");
		sb.append(companyId);
		sb.append(", userId=");
		sb.append(userId);
		sb.append(", userName=");
		sb.append(userName);
		sb.append(", createDate=");
		sb.append(createDate);
		sb.append(", modifiedDate=");
		sb.append(modifiedDate);
		sb.append(", absoluteLifetimeMinutes=");
		sb.append(absoluteLifetimeMinutes);
		sb.append(", idleTimeoutMinutes=");
		sb.append(idleTimeoutMinutes);
		sb.append("}");

		return sb.toString();
	}

	@Override
	public FIPSSessionSettings toEntityModel() {
		FIPSSessionSettingsImpl fipsSessionSettingsImpl =
			new FIPSSessionSettingsImpl();

		fipsSessionSettingsImpl.setMvccVersion(mvccVersion);
		fipsSessionSettingsImpl.setFipsSessionSettingsId(fipsSessionSettingsId);
		fipsSessionSettingsImpl.setCompanyId(companyId);
		fipsSessionSettingsImpl.setUserId(userId);

		if (userName == null) {
			fipsSessionSettingsImpl.setUserName("");
		}
		else {
			fipsSessionSettingsImpl.setUserName(userName);
		}

		if (createDate == Long.MIN_VALUE) {
			fipsSessionSettingsImpl.setCreateDate(null);
		}
		else {
			fipsSessionSettingsImpl.setCreateDate(new Date(createDate));
		}

		if (modifiedDate == Long.MIN_VALUE) {
			fipsSessionSettingsImpl.setModifiedDate(null);
		}
		else {
			fipsSessionSettingsImpl.setModifiedDate(new Date(modifiedDate));
		}

		fipsSessionSettingsImpl.setAbsoluteLifetimeMinutes(
			absoluteLifetimeMinutes);
		fipsSessionSettingsImpl.setIdleTimeoutMinutes(idleTimeoutMinutes);

		fipsSessionSettingsImpl.resetOriginalValues();

		return fipsSessionSettingsImpl;
	}

	@Override
	public void readExternal(ObjectInput objectInput) throws IOException {
		mvccVersion = objectInput.readLong();

		fipsSessionSettingsId = objectInput.readLong();

		companyId = objectInput.readLong();

		userId = objectInput.readLong();
		userName = objectInput.readUTF();
		createDate = objectInput.readLong();
		modifiedDate = objectInput.readLong();

		absoluteLifetimeMinutes = objectInput.readInt();

		idleTimeoutMinutes = objectInput.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput objectOutput) throws IOException {
		objectOutput.writeLong(mvccVersion);

		objectOutput.writeLong(fipsSessionSettingsId);

		objectOutput.writeLong(companyId);

		objectOutput.writeLong(userId);

		if (userName == null) {
			objectOutput.writeUTF("");
		}
		else {
			objectOutput.writeUTF(userName);
		}

		objectOutput.writeLong(createDate);
		objectOutput.writeLong(modifiedDate);

		objectOutput.writeInt(absoluteLifetimeMinutes);

		objectOutput.writeInt(idleTimeoutMinutes);
	}

	public long mvccVersion;
	public long fipsSessionSettingsId;
	public long companyId;
	public long userId;
	public String userName;
	public long createDate;
	public long modifiedDate;
	public int absoluteLifetimeMinutes;
	public int idleTimeoutMinutes;

}
// LIFERAY-SERVICE-BUILDER-HASH:-698137611