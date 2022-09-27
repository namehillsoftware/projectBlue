package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

interface IFilePropertiesContainerRepository {
	fun getFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>): ContainVersionedFileProperties?
	fun putFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>, filePropertiesContainer: ContainVersionedFileProperties)
}
