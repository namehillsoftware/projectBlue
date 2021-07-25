package com.lasthopesoftware.bluewater.client.connection.settings.changes.GivenALibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.FakeMessageBus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenRemovingTheLibrary : AndroidContext() {

	companion object {
		private val libraryToRemove = Library(_id = 14)
		private val messageBus = FakeMessageBus(ApplicationProvider.getApplicationContext())
		private val libraryStorage = mockk<ILibraryStorage>()
	}

	override fun before() {
		every { libraryStorage.removeLibrary(any()) } returns Unit.toPromise()

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every {
			connectionSettingsLookup.lookupConnectionSettings(LibraryId(13))
		} returns ConnectionSettings("codeOne").toPromise() andThen ConnectionSettings("codeOne").toPromise()

		val connectionSettingsChangeDetectionLibraryStorage = ObservableConnectionSettingsLibraryStorage(
			libraryStorage,
			connectionSettingsLookup,
			messageBus
		)

		connectionSettingsChangeDetectionLibraryStorage.removeLibrary(libraryToRemove).toFuture().get()
	}

	@Test
	fun thenTheLibraryIsRemoved() {
		verify { libraryStorage.removeLibrary(libraryToRemove) }
	}

	@Test
	fun thenNoUpdatesAreSentOnTheMessageBus() {
		assertThat(messageBus.recordedIntents).isEmpty()
	}
}
