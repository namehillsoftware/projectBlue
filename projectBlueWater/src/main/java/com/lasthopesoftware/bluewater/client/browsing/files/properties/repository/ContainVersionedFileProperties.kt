package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import com.lasthopesoftware.bluewater.client.browsing.files.properties.LookupFileProperties

interface ContainVersionedFileProperties {
	val revision: Long
	val properties: LookupFileProperties
	fun updateProperty(key: String, value: String)
}
