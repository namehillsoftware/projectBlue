package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.ContainVersionedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

open class FakeFilePropertiesContainerRepository : IFilePropertiesContainerRepository {
	private val storage = HashMap<UrlKeyHolder<ServiceFile>, ContainVersionedFileProperties>()
	override fun getFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>): ContainVersionedFileProperties? {
		return storage[key]
	}

	override fun putFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>, filePropertiesContainer: ContainVersionedFileProperties
	) {
		storage[key] = filePropertiesContainer
	}
}
