package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenSettingTheVolumeToSeventyPercent {


	companion object {
		private var volumeManager: NoTransformVolumeManager? = null
		private var returnedVolume = 0f
		@BeforeClass
		fun before() {
			volumeManager = NoTransformVolumeManager()
			val maxFileVolumeManager = MaxFileVolumeManager(volumeManager!!)
			maxFileVolumeManager.setMaxFileVolume(.9f)
			returnedVolume = maxFileVolumeManager.setVolume(.7f).toFuture().get()!!
		}
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsSetToTheCorrectVolume() {
		Assertions.assertThat(volumeManager!!.volume.toFuture().get()).isCloseTo(.63f, Assertions.offset(.00001f))
	}

	@Test
	fun thenTheReturnedVolumeIsSetToTheCorrectVolume() {
		Assertions.assertThat(returnedVolume).isCloseTo(.63f, Assertions.offset(.00001f))
	}
}
