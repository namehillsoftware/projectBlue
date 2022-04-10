package com.lasthopesoftware.bluewater.client.connection.builder.GivenServerIsFoundViaLookup.AndAnAuthKeyIsProvided

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenScanningForUrls {

	companion object {
		private var urlProvider: IUrlProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionTester = mockk<TestConnections>()
			every { connectionTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
			every { connectionTester.promiseIsConnectionPossible(match { a -> "http://1.2.3.4:143/MCWS/v1/" == a.urlProvider.baseUrl.toString() && "gooey" == a.urlProvider.authCode }) } returns true.toPromise()

			val serverLookup = mockk<LookupServers>()
			every { serverLookup.promiseServerInformation(LibraryId(15)) } returns Promise(
				ServerInfo(
					143,
					null,
					"1.2.3.4", emptyList(), emptyList(),
					null
				)
			)

			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(15)) } returns ConnectionSettings(accessCode = "gooPc", userName = "myuser", password = "myPass").toPromise()

			val urlScanner = UrlScanner(
				{ "gooey" },
				connectionTester,
				serverLookup,
				connectionSettingsLookup,
				OkHttpFactory
			)

			urlProvider = urlScanner.promiseBuiltUrlProvider(LibraryId(15)).toExpiringFuture().get()
		}
	}

	@Test
	fun thenTheUrlProviderIsReturned() {
		assertThat(urlProvider).isNotNull
	}

	@Test
	fun thenTheBaseUrlIsCorrect() {
		assertThat(urlProvider?.baseUrl.toString()).isEqualTo("http://1.2.3.4:143/MCWS/v1/")
	}
}
