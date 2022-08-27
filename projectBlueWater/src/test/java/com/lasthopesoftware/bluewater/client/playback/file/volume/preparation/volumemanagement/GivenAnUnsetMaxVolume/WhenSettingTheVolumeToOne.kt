package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenAnUnsetMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSettingTheVolumeToOne {

	private var volumeManager = NoTransformVolumeManager()
	private var returnedVolume = 0f

	@BeforeAll
	fun before() {
		val maxFileVolumeManager = MaxFileVolumeManager(volumeManager)
		returnedVolume = maxFileVolumeManager.setVolume(1f).toExpiringFuture().get()!!
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		Assertions.assertThat(volumeManager.volume.toExpiringFuture().get()).isEqualTo(1F)
	}

	@Test
	fun thenTheReturnedVolumeIsSetToTheMaxVolume() {
		Assertions.assertThat(returnedVolume).isEqualTo(1f)
	}
}
