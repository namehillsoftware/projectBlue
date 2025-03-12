package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFaultingConnection

import com.lasthopesoftware.bluewater.client.connection.JRiverConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import okhttp3.Callback
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URL

class WhenCheckingIfTheServerConnectionIsPossible {

	private val result by lazy {
		JRiverConnectionProvider(
			MediaServerUrlProvider("auth", "test", 80),
			mockk {
				every {
					getOkHttpClient(match { a ->
						"http://test:80/MCWS/v1/" == a.baseUrl.toString()
					})
				} answers {
					val urlProvider = firstArg<ProvideUrls>()
					spyk {
						every { newCall(match { r -> r.url.toUrl() == URL(urlProvider.baseUrl, "Alive") }) } answers {
							mockk(relaxed = true, relaxUnitFun = true) {
								val call = this
								every { enqueue(any()) } answers {
									val callback = firstArg<Callback>()
									callback.onFailure(call, IOException())
								}
							}
						}
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
