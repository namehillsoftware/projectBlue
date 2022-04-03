package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionSettingsChange

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import io.mockk.mockk
import io.mockk.verify
import org.junit.BeforeClass
import org.junit.Test

class WhenReceivingTheChange {
	companion object {
		private val connectionSessions = mockk<ManageConnectionSessions>(relaxUnitFun = true)

		@JvmStatic
		@BeforeClass
		fun before() {
			val connectionSettingsChangeReceiver = ConnectionSessionSettingsChangeReceiver(connectionSessions)
			connectionSettingsChangeReceiver(ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated(LibraryId(41)))
		}
	}

	@Test
	fun thenTheConnectionIsRemoved() {
		verify { connectionSessions.removeConnection(LibraryId(41)) }
	}
}
