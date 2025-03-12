package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItIsAlive

import com.lasthopesoftware.bluewater.client.connection.JRiverConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import okhttp3.Callback
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
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
							val request = firstArg<Request>()
							mockk(relaxed = true, relaxUnitFun = true) {
								val call = this
								every { enqueue(any()) } answers {
									val callback = firstArg<Callback>()
									val buffer = Buffer()
									buffer.write(
										"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
										<Response Status="OK">
										<Item Name="Master">1192</Item>
										<Item Name="Sync">1192</Item>
										<Item Name="LibraryStartup">1501430846</Item>
										</Response>
										""".toByteArray()
									)
									callback.onResponse(
										call,
										Response
											.Builder()
											.request(request)
											.protocol(Protocol.HTTP_1_1)
											.message("Ok")
											.code(200)
											.body(
												RealResponseBody(null, buffer.size, buffer)
											)
											.build()
									)
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
		assertThat(result).isTrue
	}
}
