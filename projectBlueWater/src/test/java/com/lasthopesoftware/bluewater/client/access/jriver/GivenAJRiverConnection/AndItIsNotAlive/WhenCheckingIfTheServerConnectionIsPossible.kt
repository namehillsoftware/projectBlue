package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItIsNotAlive

import com.lasthopesoftware.bluewater.client.connection.JRiverLibraryConnection
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class WhenCheckingIfTheServerConnectionIsPossible {

	private val result by lazy {
		val serverConnection = ServerConnection("auth", "test", 80)
		JRiverLibraryConnection(
			serverConnection,
			mockk {
				every {
					getServerClient(match { a ->
						"http://test:80/MCWS/v1/" == a.baseUrl.toString()
					})
				} answers {
					val urlProvider = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "Alive")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"K",
								"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
										<Response Status="NOT-OK">
										<Item Name="Master">1192</Item>
										<Item Name="Sync">1192</Item>
										<Item Name="LibraryStartup">1501430846</Item>
										</Response>
										""".toByteArray().inputStream()
							)
						)
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
