package com.lasthopesoftware.bluewater.client.stored.library.items.files.download

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.lasthopesoftware.resources.io.PromisingReadableStreamWrapper
import com.namehillsoftware.handoff.promises.Promise
import java.io.ByteArrayInputStream

class StoredFileDownloader(private val libraryConnections: ProvideLibraryConnections) : DownloadStoredFiles {
	override fun promiseDownload(libraryId: LibraryId, storedFile: StoredFile): Promise<PromisingReadableStream> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess {
				it?.promiseFile(ServiceFile(storedFile.serviceId))
					.keepPromise { PromisingReadableStreamWrapper(ByteArrayInputStream(emptyByteArray)) }
			}
}
