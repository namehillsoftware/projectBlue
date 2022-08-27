package com.lasthopesoftware.bluewater.client.connection.selected.GivenNoSelectedLibrary.AndTheConnectionSettingsChange

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.BeforeAll

class WhenHandlingTheConnectionSettingsChange {
	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()

	@BeforeAll
	fun act() {
		val mockSelectedLibraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { mockSelectedLibraryIdentifierProvider.selectedLibraryId } returns Promise.empty()

		val selectedConnectionSettingsChangeReceiver = SelectedConnectionSettingsChangeReceiver(
			mockSelectedLibraryIdentifierProvider,
			recordingApplicationMessageBus
		)
		selectedConnectionSettingsChangeReceiver(
			ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated(LibraryId(4))
		)
	}

	@Test
	fun `then an update is not sent on the message bus`() {
		assertThat(recordingApplicationMessageBus.recordedMessages).isEmpty()
	}
}
