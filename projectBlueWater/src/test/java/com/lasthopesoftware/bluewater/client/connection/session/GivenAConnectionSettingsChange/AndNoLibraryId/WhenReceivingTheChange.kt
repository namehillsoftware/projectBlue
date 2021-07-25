package com.lasthopesoftware.bluewater.client.connection.session.GivenAConnectionSettingsChange.AndNoLibraryId

import android.content.Intent
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenReceivingTheChange : AndroidContext() {
	companion object {
		private val connectionSessions = mockk<ManageConnectionSessions>(relaxUnitFun = true)
	}

	override fun before() {
		val connectionSettingsUpdatedIntent = Intent(ObservableConnectionSettingsLibraryStorage.connectionSettingsUpdated)

		val connectionSettingsChangeReceiver = ConnectionSessionSettingsChangeReceiver(connectionSessions)
		connectionSettingsChangeReceiver.onReceive(
			mockk(),
			connectionSettingsUpdatedIntent)
	}

	@Test
	fun thenNoConnectionIsRemoved() {
		verify(exactly = 0) { connectionSessions.removeConnection(any()) }
	}
}
