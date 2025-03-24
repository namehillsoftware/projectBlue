package com.lasthopesoftware.bluewater.client.browsing.library.settings.access

import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.namehillsoftware.handoff.promises.Promise

interface StoreLibrarySettings {
	fun promiseSavedLibrarySettings(librarySettings: LibrarySettings): Promise<LibrarySettings>
}
