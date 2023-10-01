package com.lasthopesoftware.bluewater.client.connection.settings.changes.GivenALibrary.AndTheConnectionSettingsDoNotChange

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenSavingTheLibrary {

	private val expectedLibrary = Library()
	private val messageBus = RecordingApplicationMessageBus()
	private val updatedLibrary by lazy {
		val libraryStorage = mockk<ILibraryStorage>()
		every { libraryStorage.saveLibrary(any()) } returns expectedLibrary.toPromise()

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every {
			connectionSettingsLookup.lookupConnectionSettings(LibraryId(13))
		} returns ConnectionSettings("codeOne").toPromise() andThen ConnectionSettings("codeOne").toPromise()

		val connectionSettingsChangeDetectionLibraryStorage = ObservableConnectionSettingsLibraryStorage(
			libraryStorage,
			connectionSettingsLookup,
			messageBus
		)

		connectionSettingsChangeDetectionLibraryStorage.saveLibrary(Library(id = 13)).toExpiringFuture().get()
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
