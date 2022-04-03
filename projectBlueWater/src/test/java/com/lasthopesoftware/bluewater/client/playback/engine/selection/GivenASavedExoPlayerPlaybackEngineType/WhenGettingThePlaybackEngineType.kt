package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenASavedExoPlayerPlaybackEngineType

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private val playbackEngineType by lazy {
	val applicationSettings = mockk<HoldApplicationSettings>()
	every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings(playbackEngineTypeName = "ExoPlayer"))

	val playbackEngineTypeSelectionPersistence = PlaybackEngineTypeSelectionPersistence(
		applicationSettings,
		PlaybackEngineTypeChangedBroadcaster(RecordingApplicationMessageBus())
	)
	playbackEngineTypeSelectionPersistence.selectPlaybackEngine(PlaybackEngineType.ExoPlayer)
	val selectedPlaybackEngineTypeAccess = SelectedPlaybackEngineTypeAccess(applicationSettings) { Promise.empty() }
	selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType().toFuture().get()
}

class WhenGettingThePlaybackEngineType {
    @Test
    fun thenThePlaybackEngineTypeIsExoPlayer() {
        assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer)
    }
}
