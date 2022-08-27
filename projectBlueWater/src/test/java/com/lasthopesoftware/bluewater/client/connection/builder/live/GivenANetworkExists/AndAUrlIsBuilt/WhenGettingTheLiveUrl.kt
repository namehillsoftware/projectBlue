package com.lasthopesoftware.bluewater.client.connection.builder.live.GivenANetworkExists.AndAUrlIsBuilt

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.net.URL

class WhenGettingTheLiveUrl {

	private val urlProvider by lazy {
		val urlProviderBuilder = mockk<BuildUrlProviders>()
		every { urlProviderBuilder.promiseBuiltUrlProvider(any()) } answers {
			val urlProvider = mockk<IUrlProvider>()
			every { urlProvider.baseUrl } returns URL("http://test-url")
			Promise(urlProvider)
		}

		val liveUrlProvider = LiveUrlProvider(
			{ mockk() },
			urlProviderBuilder
		)
		liveUrlProvider.promiseLiveUrl(LibraryId(10)).toExpiringFuture().get()
	}

	@Test
	fun `then the url is correct`() {
		assertThat(urlProvider!!.baseUrl.toString()).isEqualTo("http://test-url")
	}
}
