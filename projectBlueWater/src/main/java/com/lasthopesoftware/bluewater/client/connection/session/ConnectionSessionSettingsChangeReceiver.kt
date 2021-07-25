package com.lasthopesoftware.bluewater.client.connection.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage

class ConnectionSessionSettingsChangeReceiver(
	private val connectionSessions: ManageConnectionSessions
) : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		val updatedLibraryId =
			intent
				?.getIntExtra(ObservableConnectionSettingsLibraryStorage.updatedConnectionSettingsLibraryId, -1)
				?: return

		if (updatedLibraryId < 0) return

		connectionSessions.removeConnection(LibraryId(updatedLibraryId))
	}
}
