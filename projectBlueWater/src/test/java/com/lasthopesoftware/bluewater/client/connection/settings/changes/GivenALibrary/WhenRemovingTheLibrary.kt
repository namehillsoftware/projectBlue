package com.lasthopesoftware.bluewater.client.connection.settings.changes.GivenALibrary

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
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRemovingTheLibrary {

	private val libraryToRemove = Library(_id = 14)
	private val messageBus = RecordingApplicationMessageBus()
	private val libraryStorage = mockk<ILibraryStorage>()

	private val mut by lazy {
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

		connectionSettingsChangeDetectionLibraryStorage
	}

	@BeforeAll
	fun act() {
		mut.removeLibrary(libraryToRemove).toExpiringFuture().get()
	}

	@Test
	fun thenTheLibraryIsRemoved() {
		verify { libraryStorage.removeLibrary(libraryToRemove) }
	}

	@Test
	fun thenNoUpdatesAreSentOnTheMessageBus() {
		assertThat(messageBus.recordedMessages).isEmpty()
	}
}
