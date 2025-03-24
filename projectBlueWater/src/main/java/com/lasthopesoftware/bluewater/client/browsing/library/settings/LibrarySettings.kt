package com.lasthopesoftware.bluewater.client.browsing.library.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

data class LibrarySettings(
	val libraryId: LibraryId? = null,
	val libraryName: String? = null,
	val isUsingExistingFiles: Boolean = false,
	val connectionSettings: StoredMediaCenterConnectionSettings? = null,
)
