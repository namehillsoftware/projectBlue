package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenATypicalPreferenceManagerAndBroadcaster

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
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
import java.util.concurrent.TimeUnit

class WhenPersistingTheSelectedPlaybackEngine : AndroidContext() {

	companion object {
		private val fakeMessageSender = FakeMessageBus(ApplicationProvider.getApplicationContext())
		private var persistedEngineType: String? = null
	}

    override fun before() {
		val applicationSettings = mockk<HoldApplicationSettings>()
		every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings())
		every { applicationSettings.promiseUpdatedSettings(any()) } answers {
			val settings = firstArg<ApplicationSettings>()
			persistedEngineType = settings.playbackEngineTypeName
			Promise(settings)
		}

        PlaybackEngineTypeSelectionPersistence(applicationSettings, PlaybackEngineTypeChangedBroadcaster(fakeMessageSender))
			.selectPlaybackEngine(PlaybackEngineType.ExoPlayer)
			.toFuture()[1, TimeUnit.SECONDS]
    }

    @Test
    fun thenTheExoPlayerSelectionIsBroadcast() {
        assertThat(fakeMessageSender.recordedIntents.first().getStringExtra(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeKey)).isEqualTo(PlaybackEngineType.ExoPlayer.name)
    }

    @Test
    fun thenTheExoPlayerSelectionIsPersisted() {
        assertThat(persistedEngineType).isEqualTo(PlaybackEngineType.ExoPlayer.name)
    }
}
