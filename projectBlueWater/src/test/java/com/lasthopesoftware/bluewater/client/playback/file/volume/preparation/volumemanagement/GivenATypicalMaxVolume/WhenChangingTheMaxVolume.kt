package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenChangingTheMaxVolume {

	companion object {
		private var volumeManager: NoTransformVolumeManager? = null
		@JvmStatic
		@BeforeClass
		fun before() {
			volumeManager = NoTransformVolumeManager()
			val maxFileVolumeManager = MaxFileVolumeManager(volumeManager!!)
			maxFileVolumeManager.setVolume(.58f)
			maxFileVolumeManager.setMaxFileVolume(.8f)
			maxFileVolumeManager.setMaxFileVolume(.47f)
		}
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsCorrectlySet() {
		Assertions.assertThat(volumeManager!!.volume.toFuture().get()).isEqualTo(.2726f)
	}
}
