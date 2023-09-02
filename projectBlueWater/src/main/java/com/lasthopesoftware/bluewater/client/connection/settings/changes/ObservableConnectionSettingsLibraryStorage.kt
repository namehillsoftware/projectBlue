package com.lasthopesoftware.bluewater.client.connection.settings.changes

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.namehillsoftware.handoff.promises.Promise

class ObservableConnectionSettingsLibraryStorage(
	private val inner: ILibraryStorage,
	private val connectionSettingsLookup: LookupConnectionSettings,
	private val sendApplicationMessages: SendApplicationMessages
) : ILibraryStorage {

	override fun saveLibrary(library: Library): Promise<Library> {
		val promisedOriginalConnectionSettings = connectionSettingsLookup
			.lookupConnectionSettings(library.libraryId)

		return inner.saveLibrary(library).then { updatedLibrary ->
			promisedOriginalConnectionSettings
				.then { originalConnectionSettings ->
					connectionSettingsLookup
						.lookupConnectionSettings(library.libraryId)
						.then { updatedConnectionSettings ->
							if (updatedConnectionSettings != originalConnectionSettings) {
								sendApplicationMessages.sendMessage(ConnectionSettingsUpdated(library.libraryId))
							}
						}
				}

			updatedLibrary
		}
	}

	override fun removeLibrary(library: Library): Promise<Unit> = inner.removeLibrary(library)

	class ConnectionSettingsUpdated(val libraryId: LibraryId) : ApplicationMessage
}
