package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkExists.AndAServerIsFoundViaLookup.AndTheUserNameIsNull

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.live.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.PassThroughHttpResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class `When Getting The Live Connection` {

	private val serverConnection by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(62)) } returns Promise(
			ServerInfo(
				143,
				null,
				"1.2.3.4", emptySet(), emptySet(),
				emptyByteArray
			)
		)

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(62)) } returns MediaCenterConnectionSettings(
			accessCode = "gooPc",
			userName = null,
			password = null
		).toPromise()

		val connectionProvider = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(isNetworkActive = true),
			PassThroughBase64Encoder,
			serverLookup,
			connectionSettingsLookup,
			mockk {
				every {
					getServerClient(match { a ->
						"http://1.2.3.4:143" == a.baseUrl.toString() && a.authCode == null
					})
				} returns mockk {
					every { promiseResponse(URL("http://1.2.3.4:143/MCWS/v1/Alive")) } returns Promise(
						PassThroughHttpResponse(
							200,
							"Ok",
							"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<Response Status="OK">
<Item Name="Master">1192</Item>
<Item Name="Sync">1192</Item>
<Item Name="LibraryStartup">1501430846</Item>
</Response>
""".toByteArray().inputStream()
						)
					)
				}
			},
			mockk(),
		)

		connectionProvider.promiseLiveServerConnection(LibraryId(62)).toExpiringFuture().get()
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
