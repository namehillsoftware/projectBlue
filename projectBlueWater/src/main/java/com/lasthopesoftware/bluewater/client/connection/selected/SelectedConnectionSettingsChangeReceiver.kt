package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Context
import android.content.Intent
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages

class SelectedConnectionSettingsChangeReceiver(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val sendMessages: SendMessages
) : ReceiveBroadcastEvents {
	override fun onReceive(context: Context, intent: Intent) {
		val updatedLibraryId =
			intent.getIntExtra(ObservableConnectionSettingsLibraryStorage.updatedConnectionSettingsLibraryId, -1)

		selectedLibraryIdProvider.selectedLibraryId.then { l ->
			if (l?.id == updatedLibraryId) sendMessages.sendBroadcast(Intent(connectionSettingsUpdated))
		}
	}

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(SelectedConnectionSettingsChangeReceiver::class.java) }

		val connectionSettingsUpdated by lazy { magicPropertyBuilder.buildProperty("connectionSettingsUpdated") }
	}
}
