package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItIsAlive

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class WhenCheckingIfTheMediaCenterConnectionDetailsIsPossible {

	private val result by lazy {
		val mediaCenterConnectionDetails = MediaCenterConnectionDetails("auth", "test", 80)
		LiveMediaCenterConnection(
			mediaCenterConnectionDetails,
			mockk {
				every {
					promiseServerClient(mediaCenterConnectionDetails)
				} answers {
					val urlProvider = firstArg<MediaCenterConnectionDetails>()
					mockk<HttpPromiseClient> {
						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Alive")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"K",
								"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
										<Response Status="OK">
										<Item Name="Master">1192</Item>
										<Item Name="Sync">1192</Item>
										<Item Name="LibraryStartup">1501430846</Item>
										</Response>
										""".toByteArray().inputStream()
							)
						)
					}.toPromise()
				}
			},
			mockk(),
        ).promiseIsConnectionPossible().toExpiringFuture().get()!!
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isTrue
	}
}
