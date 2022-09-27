package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import java.util.concurrent.ConcurrentHashMap

class FilePropertiesContainer(override val revision: Int, properties: Map<String, String>) : ContainVersionedFileProperties {
	override val properties = ConcurrentHashMap(properties)

	override fun updateProperty(key: String, value: String) {
		properties[key] = value
	}
}
