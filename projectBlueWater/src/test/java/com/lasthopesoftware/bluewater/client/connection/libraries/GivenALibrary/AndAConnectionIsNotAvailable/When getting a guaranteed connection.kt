package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndAConnectionIsNotAvailable

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionUnavailableException
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

private const val libraryId = 881

class `When getting a guaranteed connection` {

	private val mut by lazy {
		GuaranteedLibraryConnectionProvider(
			mockk {
				every { promiseLibraryConnection(LibraryId(libraryId)) } returns Promise(null as LiveServerConnection?)
			}
		)
	}

	private var connectionUnavailableException: ConnectionUnavailableException? = null

	@BeforeAll
	fun act() {
		try {
			mut.promiseLibraryConnection(LibraryId(libraryId)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			connectionUnavailableException = e.cause as? ConnectionUnavailableException ?: throw e
		}
	}

	@Test
	fun `then a connection unavailable exception is thrown`() {
		assertThat(connectionUnavailableException).isNotNull
	}
}
