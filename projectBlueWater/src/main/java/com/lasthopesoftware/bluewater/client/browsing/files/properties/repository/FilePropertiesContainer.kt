package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import java.util.Collections

class FilePropertiesContainer(override val revision: Long, properties: Map<String, String>) : ContainVersionedFileProperties {
	private val sync = Any()

	@Volatile
	override var properties: MutableMap<String, String> = Collections.synchronizedMap(properties)

	override fun updateProperty(key: String, value: String) {
		val currentProperties = properties
		try {
			currentProperties[key] = value
		} catch (_: UnsupportedOperationException) {
			// The type system doesn't really know if a `Map` is read-only, so lazily detect if it's read-only
			// based on getting an `UnsupportedOperationException` when trying to update it.
			if (properties === currentProperties) {
				synchronized(sync) {
					if (properties === currentProperties)
						properties = Collections.synchronizedMap(properties.toMutableMap())
				}
			}

			properties[key] = value
		}
	}
}
