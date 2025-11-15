package com.lasthopesoftware.bluewater.client.connection.live.GivenANetworkDoesNotExist

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.ConfiguredActiveNetwork
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.strings.JsonEncoderDecoder
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class `When Getting The Live Connection` {

	private val urlProvider by lazy {
		val liveUrlProvider = LiveServerConnectionProvider(
			ConfiguredActiveNetwork(),
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			JsonEncoderDecoder,
			mockk(),
		)
		liveUrlProvider.promiseLiveServerConnection(LibraryId(23)).toExpiringFuture().get()
	}

	@Test
	fun `then a url provider is not returned`() {
		assertThat(urlProvider).isNull()
	}
}
