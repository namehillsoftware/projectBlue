package com.lasthopesoftware.bluewater.client.stored.library.items.files.download

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import java.io.ByteArrayInputStream
import java.io.InputStream

class StoredFileDownloader(private val libraryConnections: ProvideLibraryConnections) : DownloadStoredFiles {
	override fun promiseDownload(libraryId: LibraryId, storedFile: StoredFile): Promise<InputStream> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess {
				it?.promiseFile(ServiceFile(storedFile.serviceId)).keepPromise { ByteArrayInputStream(emptyByteArray) }
			}
}
