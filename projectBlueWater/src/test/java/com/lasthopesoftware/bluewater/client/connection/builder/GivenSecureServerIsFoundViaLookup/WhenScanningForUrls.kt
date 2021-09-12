package com.lasthopesoftware.bluewater.client.connection.builder.GivenSecureServerIsFoundViaLookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenScanningForUrls {

	companion object {
		private val urlProvider by lazy {
			val connectionTester = mockk<TestConnections>()
			every { connectionTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
			every {
				connectionTester.promiseIsConnectionPossible(match { a -> listOf(
					"https://1.2.3.4:452/MCWS/v1/",
					"http://1.2.3.4:143/MCWS/v1/").contains(a.urlProvider.baseUrl.toString()) })
			} returns true.toPromise()

			val serverLookup = mockk<LookupServers>()
			every { serverLookup.promiseServerInformation(LibraryId(35)) } returns Promise(
				ServerInfo(
					143,
					452,
					"1.2.3.4",
					emptyList(),
					emptyList(),
					"2386166660562C5AAA1253B2BED7C2483F9C2D45"
				)
			)

			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(35)) } returns ConnectionSettings(accessCode = "gooPc").toPromise()

			val urlScanner = UrlScanner(
				PassThroughBase64Encoder,
				connectionTester,
				serverLookup,
				connectionSettingsLookup,
				OkHttpFactory
			)

			urlScanner.promiseBuiltUrlProvider(LibraryId(35)).toFuture().get()
		}
	}

	@Test
	fun thenTheBaseUrlIsCorrect() {
		assertThat(urlProvider?.baseUrl.toString()).isEqualTo("https://1.2.3.4:452/MCWS/v1/")
	}

	@Test
	fun thenTheCertificateFingerprintIsCorrect() {
		assertThat(urlProvider?.certificateFingerprint)
			.isEqualTo(Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45"))
	}
}
