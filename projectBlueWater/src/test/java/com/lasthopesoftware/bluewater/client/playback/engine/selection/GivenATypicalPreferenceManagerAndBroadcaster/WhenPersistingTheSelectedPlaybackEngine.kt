package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenATypicalPreferenceManagerAndBroadcaster

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettings
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenPersistingTheSelectedPlaybackEngine {

	companion object {
		private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		private var persistedEngineType: String? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val applicationSettings = mockk<HoldApplicationSettings>()
			every { applicationSettings.promiseApplicationSettings() } returns Promise(ApplicationSettings())
			every { applicationSettings.promiseUpdatedSettings(any()) } answers {
				val settings = firstArg<ApplicationSettings>()
				persistedEngineType = settings.playbackEngineTypeName
				Promise(settings)
			}

			PlaybackEngineTypeSelectionPersistence(
				applicationSettings,
				PlaybackEngineTypeChangedBroadcaster(recordingApplicationMessageBus)
			)
				.selectPlaybackEngine(PlaybackEngineType.ExoPlayer)
				.toFuture()[1, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenTheExoPlayerSelectionIsBroadcast() {
		assertThat(
			recordingApplicationMessageBus.recordedMessages.filterIsInstance<PlaybackEngineTypeChangedBroadcaster.PlaybackEngineTypeChanged>()
				.first().playbackEngineType
		).isEqualTo(PlaybackEngineType.ExoPlayer)
	}

	@Test
	fun thenTheExoPlayerSelectionIsPersisted() {
		assertThat(persistedEngineType).isEqualTo(PlaybackEngineType.ExoPlayer.name)
	}
}
