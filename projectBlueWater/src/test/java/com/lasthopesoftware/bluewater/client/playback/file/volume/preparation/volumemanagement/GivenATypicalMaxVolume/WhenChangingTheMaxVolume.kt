package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenChangingTheMaxVolume {

	private val volumeManager = NoTransformVolumeManager()

	@BeforeAll
	fun before() {
		val maxFileVolumeManager = MaxFileVolumeManager(volumeManager)
		maxFileVolumeManager.setVolume(.58f)
		maxFileVolumeManager.setMaxFileVolume(.8f)
		maxFileVolumeManager.setMaxFileVolume(.47f)
	}

	@Test
	fun `then the playback handler volume is correctly set`() {
		assertThat(volumeManager.volume.toExpiringFuture().get()).isEqualTo(.2726f)
	}
}
