package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndTheConnectionSettingsChangeForADifferentLibrary

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionSettingsChangeReceiver
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage.Companion.connectionSettingsUpdated
import com.lasthopesoftware.resources.FakeMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenHandlingTheConnectionSettingsChange : AndroidContext() {
	companion object {
		private val messageBus = FakeMessageBus(ApplicationProvider.getApplicationContext())
	}

	override fun before() {
		val mockSelectedLibraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { mockSelectedLibraryIdentifierProvider.selectedLibraryId } returns Promise(LibraryId(22))

		val connectionSettingsUpdatedIntent = Intent(connectionSettingsUpdated)
		connectionSettingsUpdatedIntent.putExtra(
			ObservableConnectionSettingsLibraryStorage.updatedConnectionSettingsLibraryId,
			2)

		val selectedConnectionSettingsChangeReceiver = SelectedConnectionSettingsChangeReceiver(
			mockSelectedLibraryIdentifierProvider,
			messageBus
		)
		selectedConnectionSettingsChangeReceiver.onReceive(mockk(), connectionSettingsUpdatedIntent)
	}

	@Test
	fun thenAnUpdateIsSentOnTheMessageBus() {
		assertThat(messageBus.recordedIntents.map { it.action }).isEmpty()
	}
}
