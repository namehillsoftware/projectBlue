package com.lasthopesoftware.bluewater.client.connection.builder.GivenServerIsFoundViaLookup.AndTheLocalIpsCanBeConnected

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
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
	private val urlProvider by lazy {
		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(17)) } returns Promise(
			ServerInfo(
				143,
				null,
				"1.2.3.4",
				setOf(
					"53.24.19.245",
					"192.168.1.56"
				), emptySet(),
				ByteArray(0)
			)
		)

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(17)) } returns ConnectionSettings(accessCode = "gooPc").toPromise()

		val urlScanner = UrlScanner(
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
						listOf("http://192.168.1.56:143/MCWS/v1/").contains(a.baseUrl.toString())
					})
				} answers {
					val urlProvider = firstArg<ServerConnection>()
					mockk {
						every { promiseResponse(URL(urlProvider.baseUrl, "Alive")) } returns Promise(
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

		urlScanner.promiseBuiltUrlProvider(LibraryId(17)).toExpiringFuture().get()
	}

	@Test
	fun `then the url provider is returned`() {
		assertThat(urlProvider).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(urlProvider?.baseUrl.toString()).isEqualTo("http://192.168.1.56:143")
	}
}
