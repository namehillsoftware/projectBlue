package com.lasthopesoftware.bluewater.client.connection.builder.live.GivenANetworkDoesNotExist

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.net.URL

class WhenGettingTheLiveUrl {

    companion object {
        private var urlProvider: IUrlProvider? = null

        @BeforeClass
		@JvmStatic
        fun before() {
			val urlProviderBuilder = mockk<BuildUrlProviders>()
			every { urlProviderBuilder.promiseBuiltUrlProvider(LibraryId(23)) } answers {
				val urlProvider = mockk<IUrlProvider>()
				Mockito.`when`(urlProvider.baseUrl).thenReturn(URL("http://test-url"))
				Promise(urlProvider)
			}

            val liveUrlProvider = LiveUrlProvider(
                { null },
                urlProviderBuilder)
            urlProvider = liveUrlProvider.promiseLiveUrl(LibraryId(23)).toFuture().get()
        }
    }

	@Test
	fun thenAUrlProviderIsNotReturned() {
		assertThat(urlProvider).isNull()
	}
}
