package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenAnUnsetMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenSettingTheVolumeToOne {

	companion object {
		private var volumeManager: NoTransformVolumeManager? = null
		private var returnedVolume = 0f

		@JvmStatic
		@BeforeClass
		fun before() {
			volumeManager = NoTransformVolumeManager()
			val maxFileVolumeManager = MaxFileVolumeManager(volumeManager!!)
			returnedVolume = maxFileVolumeManager.setVolume(1f).toExpiringFuture().get()!!
		}
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		Assertions.assertThat(volumeManager!!.volume.toExpiringFuture().get()).isEqualTo(1F)
	}

	@Test
	fun thenTheReturnedVolumeIsSetToTheMaxVolume() {
		Assertions.assertThat(returnedVolume).isEqualTo(1f)
	}
}
