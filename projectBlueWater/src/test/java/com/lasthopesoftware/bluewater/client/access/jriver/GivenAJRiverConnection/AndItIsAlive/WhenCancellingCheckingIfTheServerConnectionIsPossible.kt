package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItIsAlive

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WhenCancellingCheckingIfTheServerConnectionIsPossible {
	private val result by lazy {
		val response = PassThroughHttpResponse(
			200,
			"Hewwo",
			("<Response Status=\"OK\">" +
				"<Item Name=\"Master\">1192</Item>" +
				"<Item Name=\"Sync\">1192</Item>" +
				"<Item Name=\"LibraryStartup\">1501430846</Item>" +
				"</Response>"
			).toByteArray().inputStream())

		val deferredResponse = object : DeferredPromise<HttpResponse>(response) {
			override fun cancellationRequested() {
				reject(CancellationException("Cancelled!"))
			}
		}

		val serverConnection = ServerConnection("auth", "test", 80)
		val promisedTest = MediaCenterConnection(
			serverConnection,
			mockk {
				every {
					getServerClient(serverConnection)
				} answers {
					val sc = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(URL(sc.baseUrl, "MCWS/v1/Alive")) } returns deferredResponse
					}
				}
			}
		).promiseIsConnectionPossible()
		promisedTest.cancel()
		deferredResponse.resolve()

		promisedTest.toExpiringFuture()[5, TimeUnit.SECONDS]
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
