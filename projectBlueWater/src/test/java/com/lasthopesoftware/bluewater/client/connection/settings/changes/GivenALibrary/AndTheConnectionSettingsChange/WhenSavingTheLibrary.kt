package com.lasthopesoftware.bluewater.client.connection.settings.changes.GivenALibrary.AndTheConnectionSettingsChange

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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSavingTheLibrary {

	private val expectedLibrary = LibrarySettings()
	private val messageBus = RecordingApplicationMessageBus()
	private var updatedLibrary: LibrarySettings? = null

	private val mut by lazy {
		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every {
			connectionSettingsLookup.lookupConnectionSettings(LibraryId(13))
		} returns MediaCenterConnectionSettings("codeOne").toPromise() andThen MediaCenterConnectionSettings("codeTwo").toPromise()

		val connectionSettingsChangeDetectionLibraryStorage = ObservableConnectionSettingsLibraryStorage(
			mockk {
				every { promiseSavedLibrarySettings(any()) } returns expectedLibrary.toPromise()
			},
			connectionSettingsLookup,
			messageBus
		)

		connectionSettingsChangeDetectionLibraryStorage
	}

	@BeforeAll
	fun act() {
		updatedLibrary = mut.promiseSavedLibrarySettings(LibrarySettings(libraryId = LibraryId(13))).toExpiringFuture().get()
	}

	@Test
	fun thenTheLibraryIsUpdated() {
		assertThat(updatedLibrary).isEqualTo(expectedLibrary)
	}

	@Test
	fun `then an update is sent on the message bus`() {
		assertThat(messageBus.recordedMessages
			.filterIsInstance<ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated>()
			.map { it.libraryId.id }).containsOnly(13)
	}
}
