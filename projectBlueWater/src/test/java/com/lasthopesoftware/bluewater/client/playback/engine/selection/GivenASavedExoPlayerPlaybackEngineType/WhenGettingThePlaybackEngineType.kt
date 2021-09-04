package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenASavedExoPlayerPlaybackEngineType

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.resources.FakeMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingThePlaybackEngineType : AndroidContext() {
    private var playbackEngineType: PlaybackEngineType? = null

    override fun before() {
		val applicationSettings = mockk<HoldApplicationSettings>()
		every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings(playbackEngineType = "ExoPlayer"))

        val playbackEngineTypeSelectionPersistence = PlaybackEngineTypeSelectionPersistence(
            applicationSettings,
            PlaybackEngineTypeChangedBroadcaster(FakeMessageBus(ApplicationProvider.getApplicationContext()))
        )
        playbackEngineTypeSelectionPersistence.selectPlaybackEngine(PlaybackEngineType.ExoPlayer)
        val selectedPlaybackEngineTypeAccess = SelectedPlaybackEngineTypeAccess(applicationSettings) { Promise.empty() }
		playbackEngineType =
            selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType().toFuture().get()
    }

    @Test
    fun thenThePlaybackEngineTypeIsExoPlayer() {
        assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer)
    }
}
