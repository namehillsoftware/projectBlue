package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ProvideLiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 405

class WhenRetrievingTheLibraryMediaCenterConnectionDetailsTwice {

	private val serverConnection = mockk<LiveServerConnection>()

	private val mut by lazy {
		val deferredConnectionSettings = DeferredPromise<ConnectionSettings?>(MediaCenterConnectionSettings(accessCode = "aB5nf"))

		val liveUrlProvider = mockk<ProvideLiveServerConnection>()
		every { liveUrlProvider.promiseLiveServerConnection(LibraryId(libraryId)) } returns serverConnection.toPromise()

		val libraryConnectionProvider = LibraryConnectionProvider(
            mockk {
				every { promiseConnectionSettings(LibraryId(libraryId)) } returns deferredConnectionSettings
			},
			NoopServerAlarm,
			liveUrlProvider,
			mockk(),
		)

		val connectionSessionManager = ConnectionSessionManager(
            libraryConnectionProvider,
			PromisedConnectionsRepository(),
			recordingApplicationMessageBus,
		)

		Pair(deferredConnectionSettings, connectionSessionManager)
	}

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
	private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
	private var connectionProvider: LiveServerConnection? = null
	private var secondConnectionProvider: LiveServerConnection? = null

	@BeforeAll
	fun before() {
		val (deferredConnectionSettings, connectionSessionManager) = mut

		val futureConnectionProvider =
			connectionSessionManager
				.promiseLibraryConnection(LibraryId(libraryId))
				.onEach(statuses::add)
				.toExpiringFuture()

		deferredConnectionSettings.resolve()

		connectionProvider = futureConnectionProvider.get()

		val secondFutureConnectionProvider =
			connectionSessionManager
				.promiseLibraryConnection(LibraryId(libraryId))
				.onEach(statuses::add)
				.toExpiringFuture()

		secondConnectionProvider = secondFutureConnectionProvider.get()
	}

	@Test
	fun `then the connection is correct`() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider)
	}

	@Test
	fun `then getting library is broadcast`() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}

	@Test
	fun `then a connection changed notification is sent`() {
		assertThat(recordingApplicationMessageBus.recordedMessages).containsExactly(
			LibraryConnectionChangedMessage(
			LibraryId(libraryId)
		))
	}
}
