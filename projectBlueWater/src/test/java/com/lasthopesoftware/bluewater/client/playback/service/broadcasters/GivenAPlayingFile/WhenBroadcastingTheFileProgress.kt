package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.GivenAPlayingFile

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.FakeMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenBroadcastingTheFileProgress {

	companion object {
		private val receivedIntent by lazy {
			val messageBus = FakeMessageBus(ApplicationProvider.getApplicationContext())
			val trackPositionBroadcaster = TrackPositionBroadcaster(messageBus, mockk())
			trackPositionBroadcaster.observeUpdates(object : PlayingFile {
				override fun promisePause(): Promise<PlayableFile> {
					return Promise.empty()
				}

				override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> {
					return object : ProgressingPromise<Duration, PlayedFile>() {
						override val progress: Promise<Duration>
							get() = Duration.ZERO.toPromise()
					}
				}

				override val duration: Promise<Duration>
					get() = Duration.standardMinutes(3).toPromise()

				override val progress: Promise<Duration>
					get() = promisePlayedFile().progress
			}).accept(
				Duration
					.standardSeconds(2)
					.plus(Duration.standardSeconds(30)))
			messageBus.recordedIntents.first()
		}

		private val duration by lazy {
			receivedIntent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration, -1)
		}

		private val progress by lazy {
			receivedIntent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
		}
	}

	@Test
	fun thenTheProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration
			.standardSeconds(2)
			.plus(Duration.standardSeconds(30)).millis)
	}

	@Test
	fun thenTheDurationIsCorrect() {
		assertThat(duration).isEqualTo(Duration.standardMinutes(3).millis)
	}
}
