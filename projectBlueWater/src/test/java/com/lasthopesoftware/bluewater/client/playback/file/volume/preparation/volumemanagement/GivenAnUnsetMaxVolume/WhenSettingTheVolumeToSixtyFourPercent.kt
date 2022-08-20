package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenAnUnsetMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.offset
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSettingTheVolumeToSixtyFourPercent {

	private val volumeManager = NoTransformVolumeManager()
	private var returnedVolume = 0f

	@BeforeAll
	fun before() {
		val maxFileVolumeManager = MaxFileVolumeManager(volumeManager)
		returnedVolume = maxFileVolumeManager.setVolume(.64f).toExpiringFuture().get()!!
	}

	@Test
	fun `then the playback handler volume is set to the correct volume`() {
		assertThat(volumeManager.volume.toExpiringFuture().get()).isCloseTo(.64f, offset(.00001f))
	}

	@Test
	fun `then the returned volume is set to the correct volume`() {
		assertThat(returnedVolume).isCloseTo(.64f, offset(.00001f))
	}
}
