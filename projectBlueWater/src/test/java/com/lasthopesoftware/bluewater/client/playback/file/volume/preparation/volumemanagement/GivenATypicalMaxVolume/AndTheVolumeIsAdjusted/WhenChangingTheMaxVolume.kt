package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.volumemanagement.GivenATypicalMaxVolume.AndTheVolumeIsAdjusted

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.offset
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenChangingTheMaxVolume {

	private val playbackHandler = NoTransformVolumeManager()

	@BeforeAll
	fun act() {
		val maxFileVolumeManager = MaxFileVolumeManager(playbackHandler)
		maxFileVolumeManager.setMaxFileVolume(.8f)
		maxFileVolumeManager.setVolume(.23f)
		maxFileVolumeManager.setMaxFileVolume(.47f)
	}

	@Test
	fun `then the playback handler volume is correctly set`() {
		assertThat(playbackHandler.volume.toExpiringFuture().get()).isCloseTo(.1081f, offset(.001f))
	}
}
