package com.lasthopesoftware.bluewater.client.connection.selected

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

class SelectedConnectionSettingsChangeReceiver(
	private val selectedLibraryIdProvider: ProvideSelectedLibraryId,
	private val sendApplicationMessages: SendApplicationMessages,
) : (ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated) -> Unit {

	override fun invoke(message: ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated) {
		selectedLibraryIdProvider.promiseSelectedLibraryId().then { l ->
			if (l == message.libraryId)
				sendApplicationMessages.sendMessage(SelectedConnectionSettingsUpdated)
		}
	}

	object SelectedConnectionSettingsUpdated : ApplicationMessage
}
