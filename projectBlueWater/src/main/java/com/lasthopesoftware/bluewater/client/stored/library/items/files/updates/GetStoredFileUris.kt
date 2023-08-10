package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import java.net.URI

interface GetStoredFileUris {
	fun promiseStoredFileUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<URI?>
}
