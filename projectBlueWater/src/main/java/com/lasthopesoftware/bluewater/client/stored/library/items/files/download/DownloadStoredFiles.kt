package com.lasthopesoftware.bluewater.client.stored.library.items.files.download

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.namehillsoftware.handoff.promises.Promise

interface DownloadStoredFiles {
    fun promiseDownload(libraryId: LibraryId, storedFile: StoredFile): Promise<PromisingReadableStream>
}
