package com.lasthopesoftware.bluewater.client.connection.builder.GivenSecureServerIsFoundViaLookup.AndTheUserHasProvidedACertificate

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val libraryId = 973

class WhenScanningForUrls {

	private val urlProvider by lazy {
		val urlScanner = UrlScanner(
			PassThroughBase64Encoder,
			mockk {
				every { promiseIsConnectionPossible(any()) } returns false.toPromise()
				every {
					promiseIsConnectionPossible(match { a ->
						listOf(
							"https://681.241.214.352:617/MCWS/v1/",
							"http://681.241.214.352:717/MCWS/v1/"
						).contains(a.urlProvider.baseUrl.toString())
					})
				} returns true.toPromise()
			},
			mockk {
				every { promiseServerInformation(LibraryId(libraryId)) } returns Promise(
					ServerInfo(
						717,
						617,
						"681.241.214.352",
						emptySet(),
						emptySet(),
						Hex.decodeHex("E5252D4CEFB873A93BA1D7017EFB47D09F8BA924")
					)
				)
			},
			mockk {
				every { lookupConnectionSettings(LibraryId(libraryId)) } returns ConnectionSettings(
					accessCode = "gooPc",
					sslCertificateFingerprint = Hex.decodeHex("F951D0C4AC2778F5C36344D7F0CD6D61E4BFE01F")
				).toPromise()
			},
			OkHttpFactory
		)

		urlScanner.promiseBuiltUrlProvider(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the base url is correct`() {
		assertThat(urlProvider?.baseUrl.toString()).isEqualTo("https://681.241.214.352:617/MCWS/v1/")
	}

	@Test
	fun `then the certificate fingerprint is correct`() {
		assertThat(urlProvider?.certificateFingerprint)
			.isEqualTo(Hex.decodeHex("E5252D4CEFB873A93BA1D7017EFB47D09F8BA924"))
	}
}
