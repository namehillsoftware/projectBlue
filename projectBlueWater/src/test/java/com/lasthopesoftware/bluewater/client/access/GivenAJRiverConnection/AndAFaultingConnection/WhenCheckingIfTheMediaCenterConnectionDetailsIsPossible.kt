package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAFaultingConnection

import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.live.LiveMediaCenterConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URL

class WhenCheckingIfTheMediaCenterConnectionDetailsIsPossible {

	private val result by lazy {
		val mediaCenterConnectionDetails = MediaCenterConnectionDetails("auth", "test", 80)
		LiveMediaCenterConnection(
			mediaCenterConnectionDetails,
			mockk {
				every {
					getServerClient(mediaCenterConnectionDetails)
				} answers {
					val urlProvider = firstArg<MediaCenterConnectionDetails>()
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
