package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenAnUnsetMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenSettingTheVolumeToSixtyFourPercent {

	companion object {
		private val volumeManager = NoTransformVolumeManager()
		private var returnedVolume = 0f

		@JvmStatic
		@BeforeClass
		fun before() {
			val maxFileVolumeManager = MaxFileVolumeManager(volumeManager)
			returnedVolume = maxFileVolumeManager.setVolume(.64f).toFuture().get()!!
		}
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsSetToTheCorrectVolume() {
		Assertions.assertThat(volumeManager.volume.toFuture().get()).isCloseTo(.64f, Assertions.offset(.00001f))
	}

	@Test
	fun thenTheReturnedVolumeIsSetToTheCorrectVolume() {
		Assertions.assertThat(returnedVolume).isCloseTo(.64f, Assertions.offset(.00001f))
	}
}
