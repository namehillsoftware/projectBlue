package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckScopedRevisions
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise

open class ScopedFilePropertiesProvider(private val scopedConnection: IConnectionProvider, private val checkScopedRevisions: CheckScopedRevisions, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideScopedFileProperties {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		scopedConnection.urlProvider.baseUrl
			?.let { filePropertiesContainerProvider.getFilePropertiesContainer(UrlKeyHolder(it, serviceFile)) }
			?.let {
				checkScopedRevisions.promiseRevision()
					.eventually { revision ->
						if (it.properties.isNotEmpty() && revision == it.revision) Promise(HashMap(it.properties))
						else FilePropertiesPromise(scopedConnection, filePropertiesContainerProvider, serviceFile, revision)
					}
			}
			?: Promise(emptyMap())
}
