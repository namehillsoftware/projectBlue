package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibrarySettings {
	fun promiseAllLibrarySettings(): Promise<Collection<LibrarySettings>>
	fun promiseLibrarySettings(libraryId: LibraryId): Promise<LibrarySettings?>
}
