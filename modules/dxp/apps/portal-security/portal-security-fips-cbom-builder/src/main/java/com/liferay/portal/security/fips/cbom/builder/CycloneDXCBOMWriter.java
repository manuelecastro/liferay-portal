/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.string.StringPool;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;

/**
 * Writes the CycloneDX 1.6 document around the components. The output is
 * reproducible: <code>version</code> is always <code>1</code>, there is no
 * timestamp, and the serial number is derived from the product version and
 * the configuration, so two builds of the same source give the same file. The
 * CycloneDX library stamps the current time into new metadata, so the writer
 * removes it.
 *
 * @author Manuele Castro
 */
public class CycloneDXCBOMWriter {

	public static final String SCHEMA_VERSION = "1.0";

	public static String write(
			List<Component> components, String configurationName,
			String productName, String productVersion)
		throws Exception {

		Bom bom = new Bom();
		Metadata metadata = new Metadata();

		Component component = new Component();

		component.setName(productName);
		component.setType(Component.Type.APPLICATION);
		component.setVersion(productVersion);

		metadata.setComponent(component);

		metadata.setProperties(
			Arrays.asList(
				new Property("liferay:cbom:configuration", configurationName),
				new Property("liferay:cbom:schema-version", SCHEMA_VERSION)));
		metadata.setTimestamp(null);

		bom.setComponents(components);
		bom.setMetadata(metadata);
		bom.setSerialNumber(
			_getSerialNumber(configurationName, productVersion));
		bom.setVersion(1);

		BomJsonGenerator bomJsonGenerator = BomGeneratorFactory.createJson(
			Version.VERSION_16, bom);

		return bomJsonGenerator.toJsonString();
	}

	private static String _getSerialNumber(
		String configurationName, String productVersion) {

		String name = productVersion + StringPool.POUND + configurationName;

		return "urn:uuid:" +
			UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
	}

}