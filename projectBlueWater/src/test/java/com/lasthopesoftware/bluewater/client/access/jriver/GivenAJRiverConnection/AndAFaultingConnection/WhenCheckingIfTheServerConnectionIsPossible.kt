package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFaultingConnection

import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.MediaCenterConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URL

class WhenCheckingIfTheServerConnectionIsPossible {

	private val result by lazy {
		val serverConnection = ServerConnection("auth", "test", 80)
		MediaCenterConnection(
			serverConnection,
			mockk {
				every {
					getServerClient(serverConnection)
				} answers {
					val urlProvider = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Alive")) } returns Promise(IOException())
					}
				}
			},
			mockk(),
		).promiseIsConnectionPossible().toExpiringFuture().get()!!
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
