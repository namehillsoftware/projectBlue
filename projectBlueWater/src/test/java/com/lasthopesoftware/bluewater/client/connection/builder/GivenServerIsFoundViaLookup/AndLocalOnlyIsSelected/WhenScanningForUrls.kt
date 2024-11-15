package com.lasthopesoftware.bluewater.client.connection.builder.GivenServerIsFoundViaLookup.AndLocalOnlyIsSelected

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenScanningForUrls {

	private val urlProvider by lazy {
		val connectionTester = mockk<TestConnections>()
		every { connectionTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
		every {
			connectionTester.promiseIsConnectionPossible(match { a ->
				listOf(
					"http://192.168.1.56:143/MCWS/v1/",
					"http://1.2.3.4:143/MCWS/v1/"
				).contains(a.urlProvider.baseUrl.toString())
			})
		} returns true.toPromise()

		val serverLookup = mockk<LookupServers>()
		every { serverLookup.promiseServerInformation(LibraryId(6)) } returns Promise(
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
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(6)) } returns ConnectionSettings(
			accessCode = "gooPc",
			isLocalOnly = true
		).toPromise()

		val urlScanner = UrlScanner(
			PassThroughBase64Encoder,
			connectionTester,
			serverLookup,
			connectionSettingsLookup,
			OkHttpFactory
		)

		urlScanner.promiseBuiltUrlProvider(LibraryId(6)).toExpiringFuture().get()
	}

	@Test
	fun `then the url provider is returned`() {
		assertThat(urlProvider).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(urlProvider?.baseUrl.toString()).isEqualTo("http://192.168.1.56:143/MCWS/v1/")
	}
}
