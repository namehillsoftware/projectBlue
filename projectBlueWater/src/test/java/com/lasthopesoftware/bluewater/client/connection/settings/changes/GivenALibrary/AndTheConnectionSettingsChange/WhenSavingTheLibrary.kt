package com.lasthopesoftware.bluewater.client.connection.settings.changes.GivenALibrary.AndTheConnectionSettingsChange

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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenSavingTheLibrary : AndroidContext() {

	companion object {
		private val expectedLibrary = Library()
		private val messageBus = FakeMessageBus(ApplicationProvider.getApplicationContext())
		private var updatedLibrary: Library? = null
	}

	override fun before() {
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

		updatedLibrary = connectionSettingsChangeDetectionLibraryStorage.saveLibrary(Library(_id = 13)).toFuture().get()
	}

	@Test
	fun thenTheLibraryIsUpdated() {
		assertThat(updatedLibrary).isEqualTo(expectedLibrary)
	}

	@Test
	fun thenAnUpdateIsSentOnTheMessageBus() {
		assertThat(messageBus.recordedIntents.map { Pair(it.action, it.getIntExtra(ObservableConnectionSettingsLibraryStorage.updatedConnectionSettingsLibraryId, -1)) })
			.containsOnly(Pair(
				ObservableConnectionSettingsLibraryStorage.connectionSettingsUpdated,
				13
			))
	}
}
