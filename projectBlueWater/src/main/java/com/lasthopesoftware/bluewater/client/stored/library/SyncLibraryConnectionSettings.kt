package com.lasthopesoftware.bluewater.client.stored.library

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class SyncLibraryConnectionSettings(private val connectionSettings: LookupConnectionSettings) : LookupConnectionSettings {
	override fun lookupConnectionSettings(libraryId: LibraryId): Promise<MediaCenterConnectionSettings?> =
		connectionSettings
			.lookupConnectionSettings(libraryId)
			.cancelBackThen { s, _ ->
				s?.copy(isLocalOnly = s.isSyncLocalConnectionsOnly)
			}
}
