package com.lasthopesoftware.bluewater.client.connection.builder.GivenAnAccessCodeThatIsAValidServer

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.PassThroughBase64Encoder
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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
			every { connectionTester.promiseIsConnectionPossible(match { a -> a.urlProvider.baseUrl.toString() == "http://gooPc:80/MCWS/v1/" }) } returns true.toPromise()

			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(14)) } returns ConnectionSettings(accessCode = "http://gooPc:80").toPromise()

			val urlScanner = UrlScanner(
				PassThroughBase64Encoder,
				connectionTester,
				mockk(),
				connectionSettingsLookup,
				OkHttpFactory.getInstance())

			urlProvider = urlScanner.promiseBuiltUrlProvider(LibraryId(14)).toFuture().get()
		}
	}

	@Test
	fun thenTheUrlProviderIsReturned() {
		assertThat(urlProvider).isNotNull
	}

	@Test
	fun thenTheBaseUrlIsCorrect() {
		assertThat(urlProvider?.baseUrl?.toString()).isEqualTo("http://gooPc:80/MCWS/v1/")
	}
}
