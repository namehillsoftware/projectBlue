package com.lasthopesoftware.bluewater.client.connection.session

import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage

class ConnectionSessionSettingsChangeReceiver(private val connectionSessions: ManageConnectionSessions) : (ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated) -> Unit {
	override fun invoke(message: ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated) {
		connectionSessions.removeConnection(message.libraryId)
	}
}
