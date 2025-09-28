package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import com.lasthopesoftware.bluewater.client.browsing.files.properties.LookupFileProperties

class FilePropertiesContainer(override val revision: Long, override val properties: LookupFileProperties) : ContainVersionedFileProperties {
	override fun updateProperty(key: String, value: String) =
		properties.update(key, value)
}
