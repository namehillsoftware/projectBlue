package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenASavedMediaPlayerPlaybackEngineType

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingThePlaybackEngineType : AndroidContext() {
	companion object {
		private val applicationSettings = FakeApplicationSettings()
		private var playbackEngineType: PlaybackEngineType? = null
	}

    override fun before() {
		val selectedPlaybackEngineTypeAccess = SelectedPlaybackEngineTypeAccess(applicationSettings)
			{ Promise(PlaybackEngineType.ExoPlayer) }

		playbackEngineType = selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType().toFuture().get()
    }

    @Test
    fun thenThePlaybackEngineTypeIsExoPlayer() {
        assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer)
    }

    @Test
    fun thenTheExoPlayerEngineIsTheSavedEngineType() {
        assertThat(applicationSettings.promiseApplicationSettings().toFuture().get()?.playbackEngineTypeName)
            .isEqualTo(PlaybackEngineType.ExoPlayer.name)
    }

	private class FakeApplicationSettings : HoldApplicationSettings {
		private var applicationSettings = ApplicationSettings(playbackEngineTypeName = "MediaPlayer")

		override fun promiseApplicationSettings(): Promise<ApplicationSettings> =
			Promise(applicationSettings)

		override fun promiseUpdatedSettings(applicationSettings: ApplicationSettings): Promise<ApplicationSettings> {
			this.applicationSettings = applicationSettings
			return Promise(applicationSettings)
		}
	}
}
