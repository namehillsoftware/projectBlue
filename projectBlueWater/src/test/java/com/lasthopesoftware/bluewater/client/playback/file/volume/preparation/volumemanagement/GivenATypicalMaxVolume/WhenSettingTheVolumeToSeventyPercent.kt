package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.offset
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSettingTheVolumeToSeventyPercent {

	private val volumeManager = NoTransformVolumeManager()
	private var returnedVolume = 0f

	@BeforeAll
	fun before() {
		val maxFileVolumeManager = MaxFileVolumeManager(volumeManager)
		maxFileVolumeManager.setMaxFileVolume(.9f)
		returnedVolume = maxFileVolumeManager.setVolume(.7f).toExpiringFuture().get()!!
	}

	@Test
	fun thenThePlaybackHandlerVolumeIsSetToTheCorrectVolume() {
		assertThat(volumeManager.volume.toExpiringFuture().get())
			.isCloseTo(.63f, offset(.00001f))
	}

	@Test
	fun thenTheReturnedVolumeIsSetToTheCorrectVolume() {
		assertThat(returnedVolume).isCloseTo(.63f, offset(.00001f))
	}
}
