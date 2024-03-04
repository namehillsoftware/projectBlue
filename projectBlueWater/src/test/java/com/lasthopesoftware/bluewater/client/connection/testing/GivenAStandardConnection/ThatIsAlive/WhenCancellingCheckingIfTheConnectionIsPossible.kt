package com.lasthopesoftware.bluewater.client.connection.testing.GivenAStandardConnection.ThatIsAlive

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class WhenCancellingCheckingIfTheConnectionIsPossible {
	private val result by lazy {
		val bufferedResponse = Buffer()
		bufferedResponse.write((
			"<Response Status=\"OK\">" +
				"<Item Name=\"Master\">1192</Item>" +
				"<Item Name=\"Sync\">1192</Item>" +
				"<Item Name=\"LibraryStartup\">1501430846</Item>" +
				"</Response>"
			).toByteArray())

		val deferredResponse = object : DeferredPromise<Response>(Response.Builder()
			.request(Request.Builder().url(URL("http://hello")).build())
			.code(200)
			.protocol(Protocol.HTTP_1_1)
			.message("Hewwo")
			.body(RealResponseBody(null, bufferedResponse.size, bufferedResponse))
			.build()) {
			override fun cancellationRequested() {
				reject(CancellationException("Cancelled!"))
			}
		}
		val connectionProvider = mockk<IConnectionProvider>()
		every { connectionProvider.promiseResponse("Alive") } returns deferredResponse

		val promisedTest = ConnectionTester.promiseIsConnectionPossible(connectionProvider)
		promisedTest.cancel()
		deferredResponse.resolve()

		promisedTest.toExpiringFuture()[5, TimeUnit.SECONDS]
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
