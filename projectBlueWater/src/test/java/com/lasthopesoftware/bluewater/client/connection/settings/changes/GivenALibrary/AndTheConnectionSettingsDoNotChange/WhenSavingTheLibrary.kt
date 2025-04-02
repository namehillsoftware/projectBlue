package com.lasthopesoftware.bluewater.client.connection.settings.changes.GivenALibrary.AndTheConnectionSettingsDoNotChange

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenSavingTheLibrary {

	private val expectedLibrary = LibrarySettings()
	private val messageBus = RecordingApplicationMessageBus()
	private val updatedLibrary by lazy {
		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every {
			connectionSettingsLookup.promiseConnectionSettings(LibraryId(13))
		} returns MediaCenterConnectionSettings("codeOne").toPromise() andThen MediaCenterConnectionSettings("codeOne").toPromise()

		val connectionSettingsChangeDetectionLibraryStorage = ObservableConnectionSettingsLibraryStorage(
			mockk {
				every { promiseSavedLibrarySettings(any()) } returns expectedLibrary.toPromise()
			},
			connectionSettingsLookup,
			messageBus
		)

		connectionSettingsChangeDetectionLibraryStorage
			.promiseSavedLibrarySettings(LibrarySettings(libraryId = LibraryId(13)))
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the library is updated`() {
		assertThat(updatedLibrary).isEqualTo(expectedLibrary)
	}

	@Test
	fun `then no updates are sent on the message bus`() {
		assertThat(messageBus.recordedMessages).isEmpty()
	}
}
