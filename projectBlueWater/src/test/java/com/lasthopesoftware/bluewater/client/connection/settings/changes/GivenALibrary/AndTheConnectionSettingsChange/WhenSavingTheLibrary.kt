package com.lasthopesoftware.bluewater.client.connection.settings.changes.GivenALibrary.AndTheConnectionSettingsChange

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
import org.junit.BeforeClass
import org.junit.Test

class WhenSavingTheLibrary {

	companion object {
		private val expectedLibrary = Library()
		private val messageBus = RecordingApplicationMessageBus()
		private var updatedLibrary: Library? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val libraryStorage = mockk<ILibraryStorage>()
			every { libraryStorage.saveLibrary(any()) } returns expectedLibrary.toPromise()

			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every {
				connectionSettingsLookup.lookupConnectionSettings(LibraryId(13))
			} returns ConnectionSettings("codeOne").toPromise() andThen ConnectionSettings("codeTwo").toPromise()

			val connectionSettingsChangeDetectionLibraryStorage = ObservableConnectionSettingsLibraryStorage(
				libraryStorage,
				connectionSettingsLookup,
				messageBus
			)

			updatedLibrary = connectionSettingsChangeDetectionLibraryStorage.saveLibrary(Library(_id = 13)).toExpiringFuture().get()
		}
	}

	@Test
	fun thenTheLibraryIsUpdated() {
		assertThat(updatedLibrary).isEqualTo(expectedLibrary)
	}

	@Test
	fun thenAnUpdateIsSentOnTheMessageBus() {
		assertThat(messageBus.recordedMessages
			.filterIsInstance<ObservableConnectionSettingsLibraryStorage.ConnectionSettingsUpdated>()
			.map { it.libraryId.id }).containsOnly(13)
	}
}
