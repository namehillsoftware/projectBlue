package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndASecondFileThatFailsToPrepareInTime

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenTheQueueIsStarted {

	private val mut by lazy {
		val serviceFiles = (0..2).map(::ServiceFile)
		val playbackPreparer = mockk<PlayableFilePreparationSource>().apply {
			every { promisePreparedPlaybackFile(ServiceFile(0), Duration.ZERO) } returns Promise(FakePreparedPlayableFile(FakeBufferingPlaybackHandler()))
			every { promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO) } returnsMany(
				listOf(
					Promise { messenger: Messenger<PreparedPlayableFile?> ->
						messenger.cancellationRequested {
							firstPromiseCancelled = true
						}
					},
					Promise(FakePreparedPlayableFile(expectedPlaybackHandler))
				)
			)
		}

		val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
		val startPosition = 0
		val queue = PreparedPlayableFileQueue(
			{ 2 },
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition)
		)

		queue
	}
	private val expectedPlaybackHandler = FakeBufferingPlaybackHandler()
	private var firstPromiseCancelled = false
	private var returnedPlaybackHandler: PlayableFile? = null

	@BeforeAll
	fun act() {
		mut.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toExpiringFuture()?.get()
		returnedPlaybackHandler = mut.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toExpiringFuture()?.get()?.playableFile
	}

	@Test
	fun `then the expected playback handler is returned`() {
		assertThat(returnedPlaybackHandler).isEqualTo(expectedPlaybackHandler)
	}

	@Test
	fun `then the first promise is cancelled`() {
		assertThat(firstPromiseCancelled).isTrue
	}
}
