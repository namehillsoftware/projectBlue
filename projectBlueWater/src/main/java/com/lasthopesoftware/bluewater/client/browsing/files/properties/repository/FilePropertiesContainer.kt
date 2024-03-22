package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import java.util.Collections

class FilePropertiesContainer(override val revision: Int, properties: Map<String, String>) : ContainVersionedFileProperties {
	override val properties: MutableMap<String, String> = Collections.synchronizedMap(HashMap(properties))

	override fun updateProperty(key: String, value: String) {
		properties[key] = value
	}
}
