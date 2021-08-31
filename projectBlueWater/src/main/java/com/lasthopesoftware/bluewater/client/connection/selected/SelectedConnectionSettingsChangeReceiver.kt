package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class SelectedConnectionSettingsChangeReceiver(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val sendMessages: SendMessages
) : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		val updatedLibraryId =
			intent?.getIntExtra(ObservableConnectionSettingsLibraryStorage.updatedConnectionSettingsLibraryId, -1)
				?: return

		selectedLibraryIdProvider.selectedLibraryId.then { l ->
			if (l?.id == updatedLibraryId) sendMessages.sendBroadcast(Intent(connectionSettingsUpdated))
		}
	}

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(SelectedConnectionSettingsChangeReceiver::class.java)

		val connectionSettingsUpdated = magicPropertyBuilder.buildProperty("connectionSettingsUpdated")
	}
}
