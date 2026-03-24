/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.oauth.client.rest.dto.v1_0;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLField;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLName;
import com.liferay.portal.vulcan.util.ObjectMapperUtil;

import jakarta.annotation.Generated;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Manuele Castro
 * @generated
 */
@Generated("")
@GraphQLName("CustomField")
@JsonFilter("Liferay.Vulcan")
@XmlRootElement(name = "CustomField")
public class CustomField implements Serializable {

	public static CustomField toDTO(String json) {
		return ObjectMapperUtil.readValue(CustomField.class, json);
	}

	public static CustomField unsafeToDTO(String json) {
		return ObjectMapperUtil.unsafeReadValue(CustomField.class, json);
	}

	@io.swagger.v3.oas.annotations.media.Schema
	public String getDefaultData() {
		if (_defaultDataSupplier != null) {
			defaultData = _defaultDataSupplier.get();

			_defaultDataSupplier = null;
		}

		return defaultData;
	}

	public void setDefaultData(String defaultData) {
		this.defaultData = defaultData;

		_defaultDataSupplier = null;
	}

	@JsonIgnore
	public void setDefaultData(
		UnsafeSupplier<String, Exception> defaultDataUnsafeSupplier) {

		_defaultDataSupplier = () -> {
			try {
				return defaultDataUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected String defaultData;

	@JsonIgnore
	private Supplier<String> _defaultDataSupplier;

	@io.swagger.v3.oas.annotations.media.Schema
	public Integer getFieldType() {
		if (_fieldTypeSupplier != null) {
			fieldType = _fieldTypeSupplier.get();

			_fieldTypeSupplier = null;
		}

		return fieldType;
	}

	public void setFieldType(Integer fieldType) {
		this.fieldType = fieldType;

		_fieldTypeSupplier = null;
	}

	@JsonIgnore
	public void setFieldType(
		UnsafeSupplier<Integer, Exception> fieldTypeUnsafeSupplier) {

		_fieldTypeSupplier = () -> {
			try {
				return fieldTypeUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected Integer fieldType;

	@JsonIgnore
	private Supplier<Integer> _fieldTypeSupplier;

	@io.swagger.v3.oas.annotations.media.Schema
	public String getFieldTypeSettings() {
		if (_fieldTypeSettingsSupplier != null) {
			fieldTypeSettings = _fieldTypeSettingsSupplier.get();

			_fieldTypeSettingsSupplier = null;
		}

		return fieldTypeSettings;
	}

	public void setFieldTypeSettings(String fieldTypeSettings) {
		this.fieldTypeSettings = fieldTypeSettings;

		_fieldTypeSettingsSupplier = null;
	}

	@JsonIgnore
	public void setFieldTypeSettings(
		UnsafeSupplier<String, Exception> fieldTypeSettingsUnsafeSupplier) {

		_fieldTypeSettingsSupplier = () -> {
			try {
				return fieldTypeSettingsUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected String fieldTypeSettings;

	@JsonIgnore
	private Supplier<String> _fieldTypeSettingsSupplier;

	@io.swagger.v3.oas.annotations.media.Schema
	public String getName() {
		if (_nameSupplier != null) {
			name = _nameSupplier.get();

			_nameSupplier = null;
		}

		return name;
	}

	public void setName(String name) {
		this.name = name;

		_nameSupplier = null;
	}

	@JsonIgnore
	public void setName(UnsafeSupplier<String, Exception> nameUnsafeSupplier) {
		_nameSupplier = () -> {
			try {
				return nameUnsafeSupplier.get();
			}
			catch (RuntimeException runtimeException) {
				throw runtimeException;
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	@GraphQLField
	@JsonProperty(access = JsonProperty.Access.READ_WRITE)
	protected String name;

	@JsonIgnore
	private Supplier<String> _nameSupplier;

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof CustomField)) {
			return false;
		}

		CustomField customField = (CustomField)object;

		return Objects.equals(toString(), customField.toString());
	}

	@Override
	public int hashCode() {
		String string = toString();

		return string.hashCode();
	}

	public String toString() {
		StringBundler sb = new StringBundler();

		sb.append("{");

		String defaultData = getDefaultData();

		if (defaultData != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"defaultData\": ");

			sb.append("\"");

			sb.append(_escape(defaultData));

			sb.append("\"");
		}

		Integer fieldType = getFieldType();

		if (fieldType != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"fieldType\": ");

			sb.append(fieldType);
		}

		String fieldTypeSettings = getFieldTypeSettings();

		if (fieldTypeSettings != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"fieldTypeSettings\": ");

			sb.append("\"");

			sb.append(_escape(fieldTypeSettings));

			sb.append("\"");
		}

		String name = getName();

		if (name != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"name\": ");

			sb.append("\"");

			sb.append(_escape(name));

			sb.append("\"");
		}

		sb.append("}");

		return sb.toString();
	}

	@io.swagger.v3.oas.annotations.media.Schema(
		accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY,
		defaultValue = "com.liferay.oauth.client.rest.dto.v1_0.CustomField",
		name = "x-class-name"
	)
	public String xClassName;

	private static String _escape(Object object) {
		return StringUtil.replace(
			String.valueOf(object), _JSON_ESCAPE_STRINGS[0],
			_JSON_ESCAPE_STRINGS[1]);
	}

	private static boolean _isArray(Object value) {
		if (value == null) {
			return false;
		}

		Class<?> clazz = value.getClass();

		return clazz.isArray();
	}

	private static String _toJSON(Map<String, ?> map) {
		StringBuilder sb = new StringBuilder("{");

		@SuppressWarnings("unchecked")
		Set set = map.entrySet();

		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, ?>> iterator = set.iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, ?> entry = iterator.next();

			sb.append("\"");
			sb.append(_escape(entry.getKey()));
			sb.append("\": ");

			Object value = entry.getValue();

			if (_isArray(value)) {
				sb.append("[");

				Object[] valueArray = (Object[])value;

				for (int i = 0; i < valueArray.length; i++) {
					if (valueArray[i] instanceof Map) {
						sb.append(_toJSON((Map<String, ?>)valueArray[i]));
					}
					else if (valueArray[i] instanceof String) {
						sb.append("\"");
						sb.append(valueArray[i]);
						sb.append("\"");
					}
					else {
						sb.append(valueArray[i]);
					}

					if ((i + 1) < valueArray.length) {
						sb.append(", ");
					}
				}

				sb.append("]");
			}
			else if (value instanceof Map) {
				sb.append(_toJSON((Map<String, ?>)value));
			}
			else if (value instanceof String) {
				sb.append("\"");
				sb.append(_escape(value));
				sb.append("\"");
			}
			else {
				sb.append(value);
			}

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

	private static final String[][] _JSON_ESCAPE_STRINGS = {
		{"\\", "\"", "\b", "\f", "\n", "\r", "\t"},
		{"\\\\", "\\\"", "\\b", "\\f", "\\n", "\\r", "\\t"}
	};

	private Map<String, Serializable> _extendedProperties;

}