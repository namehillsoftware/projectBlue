package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndASecureServerIsFoundViaLookup.AndTheLocalIpsCanBeConnected

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class `When Getting The Live Connection` {

	private val modules by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(5)) } returns Promise(
			ServerInfo(
				143,
				452,
				"1.2.3.4",
				setOf(
					"53.24.19.245",
					"192.168.1.56"
				), emptySet(),
				Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45")
			)
		)

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(5)) } returns ConnectionSettings(accessCode = "gooPc").toPromise()

		LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			connectionSettingsLookup,
			mockk {
				every { getServerClient(any()) } returns mockk {
					every { promiseResponse(any()) } returns Promise(
						PassThroughHttpResponse(
							200,
							"Ok",
							"""
							<?xml version="1.0" encoding="UTF-8" standalone="yes" ?><Response Status="FAIL"></Response>
							""".trimIndent().toByteArray().inputStream()
						)
					)
				}

				every {
					getServerClient(match { a ->
						listOf("http://192.168.1.56:143").contains(a.baseUrl.toString())
					})
				} answers {
					val urlProvider = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "MCWS/v1/Alive")) } returns Promise(
							PassThroughHttpResponse(
								200,
								"Ok",
								"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
									<Response Status="OK"></Response>""".toByteArray().inputStream()
							)
						)
					}
				}
			},
			mockk(),
		)
	}

	private var serverConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		serverConnection = modules.promiseLiveServerConnection(LibraryId(5)).toExpiringFuture().get()
	}

	@Test
	fun `then the url provider is returned`() {
		assertThat(serverConnection).isNotNull
	}

	@Test
	fun `then the non secure url is used`() {
		assertThat(serverConnection?.serverConnection?.baseUrl.toString()).isEqualTo("http://192.168.1.56:143")
	}
}
