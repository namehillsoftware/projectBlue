package com.lasthopesoftware.bluewater.client.connection.builder.GivenSecureServerIsFoundViaLookup.AndTheFingerprintIsNull

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import okhttp3.Callback
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.RealResponseBody
import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenScanningForUrls {

	private val services by lazy {

		val connectionTester = mockk<TestConnections>()
		every { connectionTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
		every { connectionTester.promiseIsConnectionPossible(match { a -> "https://1.2.3.4:452/MCWS/v1/" == a.urlProvider.baseUrl.toString() }) } returns true.toPromise()

		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(16)) } returns Promise(
			ServerInfo(
				143,
				452,
				"1.2.3.4", emptySet(), emptySet(),
				ByteArray(0),
			)
		)

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(16)) } returns ConnectionSettings(accessCode = "gooPc", userName = "user", password = null).toPromise()

		val urlScanner = UrlScanner(
			PassThroughBase64Encoder,
			serverLookup,
			connectionSettingsLookup,
			mockk {
				every {
					getOkHttpClient(match { a -> "https://1.2.3.4:452/MCWS/v1/" == a.baseUrl.toString() })
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
										<Response Status="OK"></Response>
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
		)

		urlScanner
	}

	private var urlProvider: ProvideUrls? = null

	@BeforeAll
	fun act() {
		urlProvider = services.promiseBuiltUrlProvider(LibraryId(16)).toExpiringFuture().get()
	}

	@Test
	fun `then the url provider is returned`() {
		assertThat(urlProvider).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(urlProvider?.baseUrl?.toString()).isEqualTo("https://1.2.3.4:452/MCWS/v1/")
	}

	@Test
	fun `then the certificate fingerprint is empty`() {
		assertThat(urlProvider?.certificateFingerprint).isEmpty()
	}
}
