package com.lasthopesoftware.bluewater.client.connection.selected.GivenASelectedLibrary.AndGettingTheLibraryFaults

import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnectionReservation
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class WhenRetrievingTheSelectedConnection {

	private val applicationMessageBus = RecordingApplicationMessageBus()
	private var connectionProvider: IConnectionProvider? = null
	private var exception: IOException? = null

	@BeforeAll
	fun act() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		val libraryConnections = mockk<ManageConnectionSessions>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ProvideSelectedLibraryId>()
		every { libraryIdentifierProvider.selectedLibraryId } returns Promise(LibraryId(2))

		SelectedConnectionReservation().use {
			val sessionConnection = SelectedConnection(
				applicationMessageBus,
				libraryIdentifierProvider,
				libraryConnections
			)
			val futureConnectionProvider = ExpiringFuturePromise(sessionConnection.promiseSessionConnection())
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibraryFailed)
			deferredConnectionProvider.sendRejection(IOException("OMG"))
			try {
				connectionProvider = futureConnectionProvider.get()
			} catch (e: ExecutionException) {
				if (e.cause is IOException) exception = e.cause as IOException?
			}
		}
	}

	@Test
	fun thenAConnectionProviderIsNotReturned() {
		assertThat(connectionProvider).isNull()
	}

	@Test
	fun thenAnIOExceptionIsReturned() {
		assertThat(exception).isNotNull
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(
			applicationMessageBus.recordedMessages
				.filterIsInstance<SelectedConnection.BuildSessionConnectionBroadcast>()
				.map { it.buildingConnectionStatus })
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.GettingLibraryFailed
			)
	}
}
