package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

interface ContainVersionedFileProperties {
	val revision: Int
	val properties: Map<String, String>
	fun updateProperty(key: String, value: String)
}
