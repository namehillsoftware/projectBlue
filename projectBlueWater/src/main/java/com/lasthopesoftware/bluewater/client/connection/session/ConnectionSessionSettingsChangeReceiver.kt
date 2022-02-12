package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

class ConnectionSessionSettingsChangeReceiver(private val connectionSessions: ManageConnectionSessions) : ReceiveBroadcastEvents {
	override fun onReceive(intent: Intent) {
		val updatedLibraryId = intent.getIntExtra(ObservableConnectionSettingsLibraryStorage.updatedConnectionSettingsLibraryId, -1)

		if (updatedLibraryId < 0) return

		connectionSessions.removeConnection(LibraryId(updatedLibraryId))
	}
}
