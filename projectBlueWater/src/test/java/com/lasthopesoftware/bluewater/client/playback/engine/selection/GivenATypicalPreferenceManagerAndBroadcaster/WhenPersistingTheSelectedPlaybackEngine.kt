package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenATypicalPreferenceManagerAndBroadcaster

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.features.ApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.features.access.HoldApplicationFeatureConfiguration
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenPersistingTheSelectedPlaybackEngine {

	private val recordingApplicationMessageBus = RecordingApplicationMessageBus()
	private var persistedEngineType: PlaybackEngineType? = null

	@BeforeAll
	fun before() {
		val applicationSettings = mockk<HoldApplicationFeatureConfiguration> {
			every { promiseFeatureConfiguration() } returns Promise(ApplicationFeatureConfiguration())
			every { promiseUpdatedFeatureConfiguration(any()) } answers {
				val settings = firstArg<ApplicationFeatureConfiguration>()
				persistedEngineType = settings.playbackEngineType
				Promise(settings)
			}
		}

		PlaybackEngineTypeSelectionPersistence(
			applicationSettings,
			PlaybackEngineTypeChangedBroadcaster(recordingApplicationMessageBus)
		)
			.selectPlaybackEngine(PlaybackEngineType.ExoPlayer)
			.toExpiringFuture()[1, TimeUnit.SECONDS]
	}

	@Test
	fun thenTheExoPlayerSelectionIsBroadcast() {
		// Only run this test if new playback engine types have been added.
		if (PlaybackEngineType.entries.any { e -> e != PlaybackEngineType.ExoPlayer }) {
			assertThat(
				recordingApplicationMessageBus.recordedMessages.filterIsInstance<PlaybackEngineTypeChangedBroadcaster.PlaybackEngineTypeChanged>()
					.first().playbackEngineType
			).isEqualTo(PlaybackEngineType.ExoPlayer)
		}
	}

	@Test
	fun thenTheExoPlayerSelectionIsPersisted() {
		assertThat(persistedEngineType).isEqualTo(PlaybackEngineType.ExoPlayer)
	}
}
