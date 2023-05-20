package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndGettingTheConnectionFaults

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenTestingIfTheConnectionIsActive {

	private val mut by lazy {
		val deferredConnectionProvider =
			DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val connectionSessionManager = ConnectionSessionManager(
			mockk(),
			libraryConnectionProvider,
			PromisedConnectionsRepository(),
			recordingApplicationMessageBus
		)

		Pair(deferredConnectionProvider, connectionSessionManager)
	}

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
	private var exception: IOException? = null
	private var isActive = false

	@BeforeAll
	fun act() {
		val (deferredConnectionProvider, connectionSessionManager) = mut

		val futureConnectionProvider =
			connectionSessionManager
				.promiseLibraryConnection(LibraryId(2))
				.toExpiringFuture()

		deferredConnectionProvider.sendRejection(IOException("OMG"))
		try {
			futureConnectionProvider[30, TimeUnit.SECONDS]
		} catch (e: ExecutionException) {
			if (e.cause is IOException) {
				exception = e.cause as IOException?
			}
		} catch (e: TimeoutException) {
			if (e.cause is IOException) {
				exception = e.cause as IOException?
			}
		}
		isActive = connectionSessionManager.promiseIsConnectionActive(LibraryId(2)).toExpiringFuture().get() ?: false
	}

	@Test
	fun `then the connection is not active`() {
		assertThat(isActive).isFalse
	}

	@Test
	fun `then an IOException is returned`() {
		assertThat(exception).isNotNull
	}

	@Test
	fun `then no messages are sent`() {
		assertThat(recordingApplicationMessageBus.recordedMessages).isEmpty()
	}
}
