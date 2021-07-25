package com.lasthopesoftware.bluewater.client.connection.settings.changes

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import com.namehillsoftware.handoff.promises.Promise

class ObservableConnectionSettingsLibraryStorage(
	private val inner: ILibraryStorage,
	private val connectionSettingsLookup: LookupConnectionSettings,
	private val sendMessages: SendMessages) : ILibraryStorage {

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
								val connectionSettingsUpdatedIntent = Intent(connectionSettingsUpdated)
								connectionSettingsUpdatedIntent.putExtra(
									updatedConnectionSettingsLibraryId,
									library.libraryId.id)
								sendMessages.sendBroadcast(connectionSettingsUpdatedIntent)
							}
						}
				}

			updatedLibrary
		}
	}

	override fun removeLibrary(library: Library): Promise<Unit> = inner.removeLibrary(library)

	companion object {
		val connectionSettingsUpdated = MagicPropertyBuilder.buildMagicPropertyName(
			StorageReadPermissionsRequestedBroadcaster::class.java,
			"connectionSettingsUpdated"
		)

		val updatedConnectionSettingsLibraryId = MagicPropertyBuilder.buildMagicPropertyName(
			StorageReadPermissionsRequestedBroadcaster::class.java,
			"updatedConnectionSettingsLibraryId"
		)
	}
}
