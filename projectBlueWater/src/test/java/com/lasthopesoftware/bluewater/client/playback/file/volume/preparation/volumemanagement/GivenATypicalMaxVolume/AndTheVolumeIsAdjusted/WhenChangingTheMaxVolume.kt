package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume.AndTheVolumeIsAdjusted

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenChangingTheMaxVolume {

	companion object {
		private val playbackHandler = NoTransformVolumeManager()
		@BeforeClass
		fun before() {
			val maxFileVolumeManager = MaxFileVolumeManager(playbackHandler)
			maxFileVolumeManager.setMaxFileVolume(.8f)
			maxFileVolumeManager.setVolume(.23f)
			maxFileVolumeManager.setMaxFileVolume(.47f)
		}
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsCorrectlySet() {
		Assertions.assertThat(playbackHandler.volume.toFuture().get()).isCloseTo(.1081f, Assertions.offset(.001f))
	}
}
