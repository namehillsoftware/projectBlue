package com.lasthopesoftware.bluewater.client.connection.builder.GivenAnAccessCodeThatIsAValidServer.WithAPort.AndAnHttpScheme.AndAnSslCertificate

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenScanningForUrls {

	private val services by lazy {
		val connectionTester = mockk<TestConnections>()
		every { connectionTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
		every { connectionTester.promiseIsConnectionPossible(match { a -> a.urlProvider.baseUrl.toString() == "http://read:628/MCWS/v1/" }) } returns true.toPromise()

		val connectionSettingsLookup = mockk<LookupConnectionSettings>()
		every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(195)) } returns ConnectionSettings(
			accessCode = "http://read:628",
			sslCertificateFingerprint = Hex.decodeHex("d0a3e4bf62221422a2e5dc9c479c2b36")
		).toPromise()

		val urlScanner = UrlScanner(
			PassThroughBase64Encoder,
			connectionTester,
			mockk(),
			connectionSettingsLookup,
			OkHttpFactory
		)

		urlScanner
	}

	private var urlProvider: IUrlProvider? = null

	@BeforeAll
	fun act() {
		urlProvider = services.promiseBuiltUrlProvider(LibraryId(195)).toExpiringFuture().get()
	}

	@Test
	fun `then the url provider is returned`() {
		assertThat(urlProvider).isNotNull
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(urlProvider!!.baseUrl.toString()).isEqualTo("http://read:628/MCWS/v1/")
	}

	@Test
	fun `then the certificate fingerprint is correct`() {
		assertThat(urlProvider?.certificateFingerprint).isEqualTo(Hex.decodeHex("d0a3e4bf62221422a2e5dc9c479c2b36"))
	}
}
