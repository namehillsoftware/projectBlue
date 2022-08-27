package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.GivenABasePreparationSourceProvider

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparationProvider
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheMaxQueueSize {
	private val maxQueueSize by lazy {
		val maxFileVolumePreparationProvider =
			MaxFileVolumePreparationProvider(object : IPlayableFilePreparationSourceProvider {
				override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource {
					return mockk()
				}

				override fun getMaxQueueSize(): Int {
					return 13
				}
			}, mockk())
		maxFileVolumePreparationProvider.maxQueueSize
	}

	@Test
	fun `then the queue size is the base queue size`() {
		assertThat(maxQueueSize).isEqualTo(13)
	}
}
