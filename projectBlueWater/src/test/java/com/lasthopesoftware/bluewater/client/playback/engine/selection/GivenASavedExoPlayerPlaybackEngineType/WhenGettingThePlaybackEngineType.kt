package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenASavedExoPlayerPlaybackEngineType

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingThePlaybackEngineType {

	private val playbackEngineType by lazy {
		val applicationFeatureConfiguration = mockk<HoldApplicationFeatureConfiguration> {
			every { promiseFeatureConfiguration() } returns ApplicationFeatureConfiguration(
				playbackEngineType = PlaybackEngineType.ExoPlayer,
			).toPromise()
		}
		val playbackEngineTypeSelectionPersistence = PlaybackEngineTypeSelectionPersistence(
			applicationFeatureConfiguration,
			PlaybackEngineTypeChangedBroadcaster(RecordingApplicationMessageBus())
		)
		playbackEngineTypeSelectionPersistence.selectPlaybackEngine(PlaybackEngineType.ExoPlayer)
		val selectedPlaybackEngineTypeAccess = SelectedPlaybackEngineTypeAccess(
			applicationFeatureConfiguration,
			mockk()
		)
		selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType().toExpiringFuture().get()
	}

    @Test
    fun thenThePlaybackEngineTypeIsExoPlayer() {
        assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer)
    }
}
