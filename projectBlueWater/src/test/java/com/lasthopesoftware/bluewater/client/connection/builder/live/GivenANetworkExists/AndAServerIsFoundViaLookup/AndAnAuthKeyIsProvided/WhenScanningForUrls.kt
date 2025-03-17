package com.lasthopesoftware.bluewater.client.connection.builder.live.GivenANetworkExists.AndAServerIsFoundViaLookup.AndAnAuthKeyIsProvided

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class WhenScanningForUrls {

	private val serverConnection by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(15)) } returns Promise(
			ServerInfo(
				143,
				null,
				"1.2.3.4", emptySet(), emptySet(),
				ByteArray(0)
			)
		)

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(15)) } returns ConnectionSettings(
			accessCode = "gooPc",
			userName = "myuser",
			password = "myPass"
		).toPromise()

		val urlScanner = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			{ "gooey" },
			serverLookup,
			connectionSettingsLookup,
			mockk {
				every {
					getServerClient(match { a ->
						listOf("http://1.2.3.4:143").contains(a.baseUrl.toString()) && a.authCode == "gooey"
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
			}
		)

		urlScanner.promiseLiveServerConnection(LibraryId(15)).toExpiringFuture().get()
	}

	@Test
	fun `then the server connection is returned`() {
		assertThat(serverConnection).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(serverConnection?.serverConnection?.baseUrl.toString()).isEqualTo("http://1.2.3.4:143")
	}
}
