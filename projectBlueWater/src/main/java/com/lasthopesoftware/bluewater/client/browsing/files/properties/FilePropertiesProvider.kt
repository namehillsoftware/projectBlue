package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise

class FilePropertiesProvider(
	private val libraryConnections: ProvideLibraryConnections,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerProvider: IFilePropertiesContainerRepository
) : ProvideFreshLibraryFileProperties {

	companion object {
		private val promisedEmptyProperties by lazy { Promise(emptyMap<String, String>()) }
	}

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		CancellableProxyPromise { cp ->
			libraryConnections
				.promiseLibraryConnection(libraryId)
				.also(cp::doCancel).eventually { connectionProvider ->
					if (cp.isCancelled) promisedEmptyProperties
					else connectionProvider
						?.let {
							checkRevisions
								.promiseRevision(libraryId)
								.also(cp::doCancel)
								.eventually { revision ->
									FilePropertiesPromise(
										it,
										filePropertiesContainerProvider,
										serviceFile,
										revision
									)
								}
						}
						?: promisedEmptyProperties
			}
		}
}
