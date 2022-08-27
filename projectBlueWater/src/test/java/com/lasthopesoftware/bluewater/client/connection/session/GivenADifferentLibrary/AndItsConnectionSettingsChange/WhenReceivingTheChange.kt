package com.lasthopesoftware.bluewater.client.connection.session.GivenADifferentLibrary.AndItsConnectionSettingsChange

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenReceivingTheChange {
	private lateinit var removedLibraryConnection: LibraryId

	private val mut by lazy {
		val connectionSessions = mockk<ManageConnectionSessions>().apply {
			every { removeConnection(any()) } answers {
				removedLibraryConnection = firstArg()
			}
		}
		ConnectionSessionSettingsChangeReceiver(connectionSessions)
	}

	@BeforeAll
	fun act() {
		mut(ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated(LibraryId(53)))
	}

	@Test
	fun `then the connection is removed`() {
		assertThat(removedLibraryConnection).isEqualTo(LibraryId(53))
	}
}
