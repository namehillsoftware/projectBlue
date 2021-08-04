package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.RevisionChecker
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import java.util.*

open class SessionFilePropertiesProvider(private val connectionProvider: IConnectionProvider, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideFilePropertiesForSession {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> {
		return RevisionChecker.promiseRevision(connectionProvider).eventually { revision: Int ->
			val urlKeyHolder = UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile)
			val filePropertiesContainer = filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder)

			if (filePropertiesContainer != null && filePropertiesContainer.properties.isNotEmpty() && revision == filePropertiesContainer.revision) {
				HashMap(filePropertiesContainer.properties).toPromise()
			} else {
				FilePropertiesPromise(connectionProvider, filePropertiesContainerProvider, serviceFile, revision)
			}
		}
	}

}
