package com.lasthopesoftware.bluewater.client.connection.builder.live.GivenANetworkExists.AndAUrlIsBuilt

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class WhenGettingTheLiveUrl {

	private val urlProvider by lazy {
		val urlProviderBuilder = mockk<BuildUrlProviders>()
		every { urlProviderBuilder.promiseBuiltUrlProvider(any()) } returns ServerConnection(URL("http://test-url")).toPromise()

		val liveUrlProvider = LiveServerConnectionProvider(
			mockk {
				every { isNetworkActive } returns true
			},
			urlProviderBuilder
		)
		liveUrlProvider.promiseLiveServerConnection(LibraryId(10)).toExpiringFuture().get()
	}

	@Test
	fun `then the url is correct`() {
		assertThat(urlProvider!!.baseUrl.toString()).isEqualTo("http://test-url")
	}
}
