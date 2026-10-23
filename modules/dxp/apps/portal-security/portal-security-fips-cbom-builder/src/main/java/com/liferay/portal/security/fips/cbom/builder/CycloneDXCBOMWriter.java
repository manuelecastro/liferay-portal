/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONUtil;

import java.nio.charset.StandardCharsets;

import java.util.UUID;

/**
 * Writes the CycloneDX 1.6 document around the components. The output is
 * reproducible: <code>version</code> is always <code>1</code>, there is no
 * timestamp, and the serial number is derived from the product version and
 * the configuration, so two builds of the same source give the same file.
 *
 * @author Manuele Castro
 */
public class CycloneDXCBOMWriter {

	public static final String SCHEMA_VERSION = "1.0";

	public static String write(
			JSONArray componentsJSONArray, String configurationName,
			String productName, String productVersion)
		throws Exception {

		return JSONUtil.put(
			"$schema", "http://cyclonedx.org/schema/bom-1.6.schema.json"
		).put(
			"bomFormat", "CycloneDX"
		).put(
			"components", componentsJSONArray
		).put(
			"metadata",
			JSONUtil.put(
				"component",
				JSONUtil.put(
					"name", productName
				).put(
					"type", "application"
				).put(
					"version", productVersion
				)
			).put(
				"properties",
				JSONUtil.putAll(
					JSONUtil.put(
						"name", "liferay:cbom:configuration"
					).put(
						"value", configurationName
					),
					JSONUtil.put(
						"name", "liferay:cbom:schema-version"
					).put(
						"value", SCHEMA_VERSION
					))
			)
		).put(
			"serialNumber", _getSerialNumber(configurationName, productVersion)
		).put(
			"specVersion", "1.6"
		).put(
			"version", 1
		).toString(
			4
		);
	}

	private static String _getSerialNumber(
		String configurationName, String productVersion) {

		String name = productVersion + StringPool.POUND + configurationName;

		return "urn:uuid:" +
			UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
	}

}