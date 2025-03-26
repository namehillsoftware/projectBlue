package com.lasthopesoftware.bluewater.client.browsing.library.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation

data class LibrarySettings(
	val libraryId: LibraryId? = null,
	val libraryName: String? = null,
	val isUsingExistingFiles: Boolean = false,
	val syncedFileLocation: SyncedFileLocation? = null,
	val connectionSettings: StoredConnectionSettings? = null,
)
