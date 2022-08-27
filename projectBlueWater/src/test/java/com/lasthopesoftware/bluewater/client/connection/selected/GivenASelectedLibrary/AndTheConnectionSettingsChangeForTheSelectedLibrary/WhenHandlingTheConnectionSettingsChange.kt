package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndTheConnectionSettingsChangeForTheSelectedLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenHandlingTheConnectionSettingsChange {
	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()

	private val mut by lazy {
		val mockSelectedLibraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { mockSelectedLibraryIdentifierProvider.selectedLibraryId } returns Promise(LibraryId(4))

		val selectedConnectionSettingsChangeReceiver = SelectedConnectionSettingsChangeReceiver(
			mockSelectedLibraryIdentifierProvider,
			recordingApplicationMessageBus
		)

		selectedConnectionSettingsChangeReceiver
	}

	@BeforeAll
	fun act() {
		mut(
			ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated(
				LibraryId(4)
			)
		)
	}

	@Test
	fun `then an update is sent on the message bus`() {
		assertThat(recordingApplicationMessageBus.recordedMessages.singleOrNull())
			.isInstanceOf(SelectedConnectionSettingsChangeReceiver.SelectedConnectionSettingsUpdated::class.java)
	}
}
