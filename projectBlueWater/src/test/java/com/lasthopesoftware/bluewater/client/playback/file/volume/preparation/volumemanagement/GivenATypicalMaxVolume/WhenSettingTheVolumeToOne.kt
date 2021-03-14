package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
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
			maxFileVolumeManager.setMaxFileVolume(.8f)
			returnedVolume = maxFileVolumeManager.setVolume(1f).toFuture().get()!!
		}
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		Assertions.assertThat(volumeManager!!.volume.toFuture().get()).isEqualTo(.8f)
	}

	@Test
	fun thenTheReturnedVolumeIsSetToTheMaxVolume() {
		Assertions.assertThat(returnedVolume).isEqualTo(.8f)
	}
}
