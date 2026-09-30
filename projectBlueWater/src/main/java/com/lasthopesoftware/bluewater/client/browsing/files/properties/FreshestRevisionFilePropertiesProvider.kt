package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.CheckRevisions
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlin.coroutines.cancellation.CancellationException

class FreshestRevisionFilePropertiesProvider(
	private val inner: ProvideFreshLibraryFileProperties,
	private val urlKeys: ProvideUrlKey,
	private val checkRevisions: CheckRevisions,
	private val filePropertiesContainerProvider: IFilePropertiesContainerRepository
) : ProvideFreshLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<LookupFileProperties> =
		ProxiedFileProperties(libraryId, serviceFile)

	private inner class ProxiedFileProperties(private val libraryId: LibraryId, private val serviceFile: ServiceFile) :
		Promise.Proxy<LookupFileProperties>() {
		init {
			val promisedUrlKey = urlKeys.promiseUrlKey(libraryId, serviceFile).also(::doCancel)
			val promisedRevision = checkRevisions.promiseRevision(libraryId).also(::doCancel)
			proxy(
				inner
					.promiseFileProperties(libraryId, serviceFile)
					.also(::doCancel)
					.eventually { properties ->
						promisedUrlKey.eventually({ urlKeyHolder ->
							if (urlKeyHolder == null || isCancelled) properties.toPromise()
							else promisedRevision.then({ revision ->
								filePropertiesContainerProvider.putFilePropertiesContainer(
									urlKeyHolder,
									FilePropertiesContainer(revision, properties)
								)
								properties
							}, { properties })
						}, { properties.toPromise() })
					}
			)
		}
	}

	private fun <T> promiseFilePropertiesCancelled(libraryId: LibraryId, serviceFile: ServiceFile) =
		Promise<T>(FilePropertiesCancellationException(libraryId, serviceFile))

	private class FilePropertiesCancellationException(libraryId: LibraryId, serviceFile: ServiceFile) :
		CancellationException("Getting file properties cancelled for $libraryId and $serviceFile.")
}
