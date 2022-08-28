package com.lasthopesoftware.bluewater.client.browsing.files.properties.repository

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder

/**
 * Created by david on 3/14/17.
 */
interface IFilePropertiesContainerRepository {
	fun getFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>): FilePropertiesContainer?
	fun putFilePropertiesContainer(key: UrlKeyHolder<ServiceFile>, filePropertiesContainer: FilePropertiesContainer)
}
