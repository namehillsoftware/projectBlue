package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import io.reactivex.Observable

interface ControlLibrarySyncs {
	fun observeLibrarySync(libraryId: LibraryId): Observable<StoredFileJobStatus>
}
