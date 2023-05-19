package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive.AndTheConnectionIsLost

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostNotification
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.session.initialization.LibraryConnectionChangedMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

private const val libraryId = 924

class `When getting a tested connection` {

	private val mut by lazy {
		val goodConnectionProvider = FakeConnectionProvider()

		val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
		every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(libraryId)) } returnsMany listOf(
			ProgressingPromise(goodConnectionProvider),
			ProgressingPromise(IOException("cure")),
		)

		val connectionSessionManager = ConnectionSessionManager(
			mockk {
				every { promiseIsConnectionPossible(goodConnectionProvider) } returns false.toPromise()
			},
			libraryConnectionProvider,
			PromisedConnectionsRepository(),
			recordingApplicationMessageBus
		)

		connectionSessionManager
	}

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
	private var exception: IOException? = null
	private var isActive = false

	@BeforeAll
	fun act() {
		val connectionSessionManager = mut

		connectionSessionManager
			.promiseLibraryConnection(LibraryId(libraryId))
			.toExpiringFuture()
			.get()

		try {
			connectionSessionManager
				.promiseTestedLibraryConnection(LibraryId(libraryId))
				.toExpiringFuture()
				.get()
		} catch (e: ExecutionException) {
			exception = e.cause as? IOException ?: throw e
		}
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
	fun `then the correct messages are sent`() {
		assertThat(recordingApplicationMessageBus.recordedMessages).containsExactly(
			LibraryConnectionChangedMessage(LibraryId(libraryId)),
			ConnectionLostNotification(LibraryId(libraryId))
		)
	}
}
