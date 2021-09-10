package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 3/7/16.
 */
open class ScopedCachedFilePropertiesProvider(
    private val connectionProvider: IConnectionProvider,
    private val filePropertiesContainerRepository: IFilePropertiesContainerRepository,
    private val scopedFilePropertiesProvider: ProvideScopedFileProperties
) : ProvideScopedFileProperties {
    override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		connectionProvider.urlProvider.baseUrl?.let {
			val urlKeyHolder = UrlKeyHolder(it, serviceFile)
			val filePropertiesContainer = filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder)
			filePropertiesContainer?.properties?.toPromise()
		} ?: scopedFilePropertiesProvider.promiseFileProperties(serviceFile)
}
