package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository

import java.util.concurrent.ConcurrentHashMap

class FilePropertiesContainer(val revision: Int, properties: Map<String, String>) {
	val properties: ConcurrentHashMap<String, String> = ConcurrentHashMap(properties)

	fun updateProperty(key: String, value: String) {
		properties[key] = value
	}
}
