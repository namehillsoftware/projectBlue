package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import java.util.Collections

class FilePropertiesContainer(override val revision: Long, properties: Map<String, String>) : ContainVersionedFileProperties {
	private val sync = Any()

	@Volatile
	override var properties: MutableMap<String, String> = Collections.synchronizedMap(
		if (properties is MutableMap) properties
		else properties.toMutableMap())

	override fun updateProperty(key: String, value: String) {
		try {
			properties[key] = value
		} catch (e: UnsupportedOperationException) {
			// The type system lied to us, this was a read-only map. We need to clean-up its mess.
			synchronized(sync) {
				properties = Collections.synchronizedMap(properties.toMutableMap())
				properties[key] = value
			}
		}
	}
}
