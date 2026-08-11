/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.model;

import com.liferay.portal.kernel.model.ModelWrapper;
import com.liferay.portal.kernel.model.wrapper.BaseModelWrapper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class is a wrapper for {@link FIPSSessionSettings}.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see FIPSSessionSettings
 * @generated
 */
public class FIPSSessionSettingsWrapper
	extends BaseModelWrapper<FIPSSessionSettings>
	implements FIPSSessionSettings, ModelWrapper<FIPSSessionSettings> {

	public FIPSSessionSettingsWrapper(FIPSSessionSettings fipsSessionSettings) {
		super(fipsSessionSettings);
	}

	@Override
	public Map<String, Object> getModelAttributes() {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put("mvccVersion", getMvccVersion());
		attributes.put("fipsSessionSettingsId", getFipsSessionSettingsId());
		attributes.put("companyId", getCompanyId());
		attributes.put("userId", getUserId());
		attributes.put("userName", getUserName());
		attributes.put("createDate", getCreateDate());
		attributes.put("modifiedDate", getModifiedDate());
		attributes.put("absoluteLifetimeMinutes", getAbsoluteLifetimeMinutes());
		attributes.put("idleTimeoutMinutes", getIdleTimeoutMinutes());

		return attributes;
	}

	@Override
	public void setModelAttributes(Map<String, Object> attributes) {
		Long mvccVersion = (Long)attributes.get("mvccVersion");

		if (mvccVersion != null) {
			setMvccVersion(mvccVersion);
		}

		Long fipsSessionSettingsId = (Long)attributes.get(
			"fipsSessionSettingsId");

		if (fipsSessionSettingsId != null) {
			setFipsSessionSettingsId(fipsSessionSettingsId);
		}

		Long companyId = (Long)attributes.get("companyId");

		if (companyId != null) {
			setCompanyId(companyId);
		}

		Long userId = (Long)attributes.get("userId");

		if (userId != null) {
			setUserId(userId);
		}

		String userName = (String)attributes.get("userName");

		if (userName != null) {
			setUserName(userName);
		}

		Date createDate = (Date)attributes.get("createDate");

		if (createDate != null) {
			setCreateDate(createDate);
		}

		Date modifiedDate = (Date)attributes.get("modifiedDate");

		if (modifiedDate != null) {
			setModifiedDate(modifiedDate);
		}

		Integer absoluteLifetimeMinutes = (Integer)attributes.get(
			"absoluteLifetimeMinutes");

		if (absoluteLifetimeMinutes != null) {
			setAbsoluteLifetimeMinutes(absoluteLifetimeMinutes);
		}

		Integer idleTimeoutMinutes = (Integer)attributes.get(
			"idleTimeoutMinutes");

		if (idleTimeoutMinutes != null) {
			setIdleTimeoutMinutes(idleTimeoutMinutes);
		}
	}

	@Override
	public FIPSSessionSettings cloneWithOriginalValues() {
		return wrap(model.cloneWithOriginalValues());
	}

	/**
	 * Returns the absolute lifetime minutes of this fips session settings.
	 *
	 * @return the absolute lifetime minutes of this fips session settings
	 */
	@Override
	public int getAbsoluteLifetimeMinutes() {
		return model.getAbsoluteLifetimeMinutes();
	}

	/**
	 * Returns the company ID of this fips session settings.
	 *
	 * @return the company ID of this fips session settings
	 */
	@Override
	public long getCompanyId() {
		return model.getCompanyId();
	}

	/**
	 * Returns the create date of this fips session settings.
	 *
	 * @return the create date of this fips session settings
	 */
	@Override
	public Date getCreateDate() {
		return model.getCreateDate();
	}

	/**
	 * Returns the fips session settings ID of this fips session settings.
	 *
	 * @return the fips session settings ID of this fips session settings
	 */
	@Override
	public long getFipsSessionSettingsId() {
		return model.getFipsSessionSettingsId();
	}

	/**
	 * Returns the idle timeout minutes of this fips session settings.
	 *
	 * @return the idle timeout minutes of this fips session settings
	 */
	@Override
	public int getIdleTimeoutMinutes() {
		return model.getIdleTimeoutMinutes();
	}

	/**
	 * Returns the modified date of this fips session settings.
	 *
	 * @return the modified date of this fips session settings
	 */
	@Override
	public Date getModifiedDate() {
		return model.getModifiedDate();
	}

	/**
	 * Returns the mvcc version of this fips session settings.
	 *
	 * @return the mvcc version of this fips session settings
	 */
	@Override
	public long getMvccVersion() {
		return model.getMvccVersion();
	}

	/**
	 * Returns the primary key of this fips session settings.
	 *
	 * @return the primary key of this fips session settings
	 */
	@Override
	public long getPrimaryKey() {
		return model.getPrimaryKey();
	}

	/**
	 * Returns the user ID of this fips session settings.
	 *
	 * @return the user ID of this fips session settings
	 */
	@Override
	public long getUserId() {
		return model.getUserId();
	}

	/**
	 * Returns the user name of this fips session settings.
	 *
	 * @return the user name of this fips session settings
	 */
	@Override
	public String getUserName() {
		return model.getUserName();
	}

	/**
	 * Returns the user uuid of this fips session settings.
	 *
	 * @return the user uuid of this fips session settings
	 */
	@Override
	public String getUserUuid() {
		return model.getUserUuid();
	}

	@Override
	public void persist() {
		model.persist();
	}

	/**
	 * Sets the absolute lifetime minutes of this fips session settings.
	 *
	 * @param absoluteLifetimeMinutes the absolute lifetime minutes of this fips session settings
	 */
	@Override
	public void setAbsoluteLifetimeMinutes(int absoluteLifetimeMinutes) {
		model.setAbsoluteLifetimeMinutes(absoluteLifetimeMinutes);
	}

	/**
	 * Sets the company ID of this fips session settings.
	 *
	 * @param companyId the company ID of this fips session settings
	 */
	@Override
	public void setCompanyId(long companyId) {
		model.setCompanyId(companyId);
	}

	/**
	 * Sets the create date of this fips session settings.
	 *
	 * @param createDate the create date of this fips session settings
	 */
	@Override
	public void setCreateDate(Date createDate) {
		model.setCreateDate(createDate);
	}

	/**
	 * Sets the fips session settings ID of this fips session settings.
	 *
	 * @param fipsSessionSettingsId the fips session settings ID of this fips session settings
	 */
	@Override
	public void setFipsSessionSettingsId(long fipsSessionSettingsId) {
		model.setFipsSessionSettingsId(fipsSessionSettingsId);
	}

	/**
	 * Sets the idle timeout minutes of this fips session settings.
	 *
	 * @param idleTimeoutMinutes the idle timeout minutes of this fips session settings
	 */
	@Override
	public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
		model.setIdleTimeoutMinutes(idleTimeoutMinutes);
	}

	/**
	 * Sets the modified date of this fips session settings.
	 *
	 * @param modifiedDate the modified date of this fips session settings
	 */
	@Override
	public void setModifiedDate(Date modifiedDate) {
		model.setModifiedDate(modifiedDate);
	}

	/**
	 * Sets the mvcc version of this fips session settings.
	 *
	 * @param mvccVersion the mvcc version of this fips session settings
	 */
	@Override
	public void setMvccVersion(long mvccVersion) {
		model.setMvccVersion(mvccVersion);
	}

	/**
	 * Sets the primary key of this fips session settings.
	 *
	 * @param primaryKey the primary key of this fips session settings
	 */
	@Override
	public void setPrimaryKey(long primaryKey) {
		model.setPrimaryKey(primaryKey);
	}

	/**
	 * Sets the user ID of this fips session settings.
	 *
	 * @param userId the user ID of this fips session settings
	 */
	@Override
	public void setUserId(long userId) {
		model.setUserId(userId);
	}

	/**
	 * Sets the user name of this fips session settings.
	 *
	 * @param userName the user name of this fips session settings
	 */
	@Override
	public void setUserName(String userName) {
		model.setUserName(userName);
	}

	/**
	 * Sets the user uuid of this fips session settings.
	 *
	 * @param userUuid the user uuid of this fips session settings
	 */
	@Override
	public void setUserUuid(String userUuid) {
		model.setUserUuid(userUuid);
	}

	@Override
	public String toXmlString() {
		return model.toXmlString();
	}

	@Override
	protected FIPSSessionSettingsWrapper wrap(
		FIPSSessionSettings fipsSessionSettings) {

		return new FIPSSessionSettingsWrapper(fipsSessionSettings);
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:624542002