package com.lasthopesoftware.bluewater.client.connection.builder.live.GivenANetworkExists.AndThUrlIsNotBuilt

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheLiveUrl {

	private val urlProvider by lazy {
		val builder = mockk<BuildUrlProviders>()
		every { builder.promiseBuiltUrlProvider(any()) } returns Promise.empty()

		val liveUrlProvider = LiveUrlProvider(
			{ mockk() },
			builder
		)
		liveUrlProvider.promiseLiveUrl(LibraryId(54)).toExpiringFuture().get()
	}

	@Test
	fun `then the url is correct`() = assertThat(urlProvider).isNull()
}
