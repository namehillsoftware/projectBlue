package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlin.coroutines.cancellation.CancellationException

class FilePropertiesProvider(
	private val libraryConnections: ProvideGuaranteedLibraryConnections,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerProvider: IFilePropertiesContainerRepository
) : ProvideFreshLibraryFileProperties {

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		ProxiedFileProperties(libraryId, serviceFile)

	private inner class ProxiedFileProperties(private val libraryId: LibraryId, private val serviceFile: ServiceFile) :
		Promise.Proxy<Map<String, String>>() {
		init {
			proxy(
				libraryConnections
					.promiseKey(libraryId, serviceFile)
					.also(::doCancel)
					.eventually { urlKeyHolder ->
						if (isCancelled) promiseFilePropertiesCancelled(libraryId, serviceFile)
						else checkRevisions
							.promiseRevision(libraryId)
							.also(::doCancel)
							.eventually { revision ->
								urlKeyHolder
									.let(filePropertiesContainerProvider::getFilePropertiesContainer)
									?.takeIf { it.properties.isNotEmpty() && revision == it.revision }
									?.properties
									?.toPromise()
									?: libraryConnections
										.promiseLibraryAccess(libraryId)
										.also(::doCancel)
										.eventually { access ->
											if (isCancelled) promiseFilePropertiesCancelled(libraryId, serviceFile)
											else access
												.promiseFileProperties(serviceFile)
												.also(::doCancel)
												.then { properties ->
													filePropertiesContainerProvider.putFilePropertiesContainer(
														urlKeyHolder,
														FilePropertiesContainer(revision, properties)
													)
													properties
												}
										}
							}
					}
			)
		}
	}

	private fun <T> promiseFilePropertiesCancelled(libraryId: LibraryId, serviceFile: ServiceFile) =
		Promise<T>(FilePropertiesCancellationException(libraryId, serviceFile))

	private class FilePropertiesCancellationException(libraryId: LibraryId, serviceFile: ServiceFile) :
		CancellationException("Getting file properties cancelled for $libraryId and $serviceFile.")
}
