package com.lasthopesoftware.bluewater.client.connection.settings.changes

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.StoreLibrarySettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class ObservableConnectionSettingsLibraryStorage(
	private val inner: StoreLibrarySettings,
	private val connectionSettingsLookup: LookupConnectionSettings,
	private val sendApplicationMessages: SendApplicationMessages
) : StoreLibrarySettings {

	override fun promiseSavedLibrarySettings(librarySettings: LibrarySettings): Promise<LibrarySettings> {
		val promisedOriginalConnectionSettings = librarySettings.libraryId?.let(connectionSettingsLookup::lookupConnectionSettings)

		return inner.promiseSavedLibrarySettings(librarySettings).then { updatedLibrary ->
			promisedOriginalConnectionSettings
				?.then { originalConnectionSettings ->
					connectionSettingsLookup
						.lookupConnectionSettings(librarySettings.libraryId)
						.then { updatedConnectionSettings ->
							if (updatedConnectionSettings != originalConnectionSettings) {
								sendApplicationMessages.sendMessage(ConnectionSettingsUpdated(librarySettings.libraryId))
							}
						}
				}

			updatedLibrary
		}
	}

	class ConnectionSettingsUpdated(val libraryId: LibraryId) : ApplicationMessage
}
