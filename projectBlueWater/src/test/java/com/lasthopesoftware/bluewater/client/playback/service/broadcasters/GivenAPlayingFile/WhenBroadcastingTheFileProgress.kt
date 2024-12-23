package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.TrackPositionUpdate
import com.lasthopesoftware.promises.extensions.ProgressedPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test

class WhenBroadcastingTheFileProgress {

	private val receivedMessage by lazy {
		val appMessageBus = RecordingApplicationMessageBus()
		val trackPositionBroadcaster = TrackPositionBroadcaster(appMessageBus, mockk())
		trackPositionBroadcaster.observeUpdates(object : PlayingFile {
			override fun promisePause(): Promise<PlayableFile> {
				return Promise.empty()
			}

			override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> {
				return object : ProgressedPromise<Duration, PlayedFile>() {
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

		appMessageBus.recordedMessages.first() as? TrackPositionUpdate
	}

	private val duration by lazy {
		receivedMessage?.fileDuration
	}

	private val progress by lazy {
		receivedMessage?.filePosition
	}

	@Test
	fun `then the progress is correct`() {
		assertThat(progress).isEqualTo(Duration
			.standardSeconds(2)
			.plus(Duration.standardSeconds(30)))
	}

	@Test
	fun `then the duration is correct`() {
		assertThat(duration).isEqualTo(Duration.standardMinutes(3))
	}
}
