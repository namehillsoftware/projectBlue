package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSettingTheVolumeToOne {

	private val volumeManager = NoTransformVolumeManager()
	private var returnedVolume = 0f

	@BeforeAll
	fun before() {
		val maxFileVolumeManager = MaxFileVolumeManager(volumeManager)
		maxFileVolumeManager.setMaxFileVolume(.8f)
		returnedVolume = maxFileVolumeManager.setVolume(1f).toExpiringFuture().get()!!
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		assertThat(volumeManager.volume.toExpiringFuture().get()).isEqualTo(.8f)
	}

	@Test
	fun thenTheReturnedVolumeIsSetToTheMaxVolume() {
		assertThat(returnedVolume).isEqualTo(.8f)
	}
}
