package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFaultingConnection

import com.lasthopesoftware.bluewater.client.connection.JRiverConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
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
		JRiverConnectionProvider(
			serverConnection,
			mockk {
				every {
					getServerClient(match { a ->
						"http://test:80/MCWS/v1/" == a.baseUrl.toString()
					})
				} answers {
					val urlProvider = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(match { it == URL(urlProvider.baseUrl, "Alive") }) } returns Promise(IOException())
					}
				}
			}
		).promiseIsConnectionPossible().toExpiringFuture().get()!!
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
