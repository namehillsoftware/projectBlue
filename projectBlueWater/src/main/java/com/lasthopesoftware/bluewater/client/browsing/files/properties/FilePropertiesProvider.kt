package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.namehillsoftware.handoff.promises.Promise
import kotlin.coroutines.cancellation.CancellationException

class FilePropertiesProvider(
	private val libraryConnections: ProvideGuaranteedLibraryConnections
) : ProvideFreshLibraryFileProperties {

	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> =
		ProxiedFileProperties(libraryId, serviceFile)

	private inner class ProxiedFileProperties(private val libraryId: LibraryId, private val serviceFile: ServiceFile) :
		Promise.Proxy<Map<String, String>>() {
		init {
			proxy(
				if (isCancelled) promiseFilePropertiesCancelled(libraryId, serviceFile)
				else libraryConnections
					.promiseLibraryAccess(libraryId)
					.also(::doCancel)
					.eventually { access ->
						if (isCancelled) promiseFilePropertiesCancelled(libraryId, serviceFile)
						else access.promiseFileProperties(serviceFile)
					}
			)
		}
	}

	private fun <T> promiseFilePropertiesCancelled(libraryId: LibraryId, serviceFile: ServiceFile) =
		Promise<T>(FilePropertiesCancellationException(libraryId, serviceFile))

	private class FilePropertiesCancellationException(libraryId: LibraryId, serviceFile: ServiceFile) :
		CancellationException("Getting file properties cancelled for $libraryId and $serviceFile.")
}
