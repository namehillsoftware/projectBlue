package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.coverart

import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.emptyByteArray
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When creating the now playing cover art view model` {
	private val services by lazy {
		val deferredDefaultImage = DeferredPromise(byteArrayOf(576.toByte()))

		Pair(
			deferredDefaultImage,
			NowPlayingCoverArtViewModel(
				RecordingApplicationMessageBus(),
				mockk(),
				mockk(),
				mockk {
					every { promiseImageBytes() } returns deferredDefaultImage
				},
				mockk(),
				mockk(),
			)
		)
	}

	private val imageLoadingStates = mutableListOf<Boolean>()
	private val loadedImages = mutableListOf<ByteArray>()

	@BeforeAll
	fun act() {
		val (deferredDefaultImage, viewModel) = services

		viewModel.isNowPlayingImageLoading.mapNotNull().subscribe(imageLoadingStates::add).toCloseable().use {
			viewModel.nowPlayingImage.mapNotNull().subscribe(loadedImages::add).toCloseable().use {
				// Simulate that the default image may not load immediately
				deferredDefaultImage.resolve()
			}
		}
	}

	@Test
	fun `then the image loading states are correct`() {
		assertThat(imageLoadingStates).containsExactly(false)
	}

	@Test
	fun `then the loaded images are correct`() {
		assertThat(loadedImages).containsExactly(
			emptyByteArray,
			byteArrayOf(576.toByte()),
		)
	}
}
