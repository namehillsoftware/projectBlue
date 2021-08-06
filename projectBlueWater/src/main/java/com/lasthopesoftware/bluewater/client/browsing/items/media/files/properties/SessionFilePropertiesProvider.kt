package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckSessionRevisions
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise

open class SessionFilePropertiesProvider(private val checkSessionRevisions: CheckSessionRevisions, private val connectionProvider: IConnectionProvider, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideFilePropertiesForSession {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> {
		return checkSessionRevisions.promiseRevision().eventually { revision ->
			connectionProvider.urlProvider.baseUrl
				?.let { filePropertiesContainerProvider.getFilePropertiesContainer(UrlKeyHolder(it, serviceFile)) }
				?.takeIf { it.properties.isNotEmpty() && revision == it.revision }
				?.let { Promise(HashMap(it.properties)) }
				?: FilePropertiesPromise(connectionProvider, filePropertiesContainerProvider, serviceFile, revision)
		}
	}
}
