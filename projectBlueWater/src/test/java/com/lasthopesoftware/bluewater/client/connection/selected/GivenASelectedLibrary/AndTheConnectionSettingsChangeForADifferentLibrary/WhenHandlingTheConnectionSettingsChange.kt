package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndTheConnectionSettingsChangeForADifferentLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenHandlingTheConnectionSettingsChange {
	companion object {
		private val recordingApplicationMessageBus = RecordingApplicationMessageBus()

		@JvmStatic
		@BeforeClass
		fun before() {
			val mockSelectedLibraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
			every { mockSelectedLibraryIdentifierProvider.selectedLibraryId } returns Promise(LibraryId(22))

			val selectedConnectionSettingsChangeReceiver = SelectedConnectionSettingsChangeReceiver(
				mockSelectedLibraryIdentifierProvider,
				recordingApplicationMessageBus
			)

			selectedConnectionSettingsChangeReceiver(
				ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated(
					LibraryId(2)
				)
			)
		}
	}

	@Test
	fun thenAnUpdateIsNotSentOnTheMessageBus() {
		assertThat(recordingApplicationMessageBus.recordedMessages).isEmpty()
	}
}
