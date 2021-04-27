package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndASecondFileThatThrowsAnExceptionOnPreparation

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.ExecutionException

class WhenTheQueueIsStarted {

	companion object {
		private val expectedPlaybackHandler = FakeBufferingPlaybackHandler()
		private var returnedPlaybackHandler: PlayableFile? = null
		private var error: Throwable? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val serviceFiles = intArrayOf(0, 1, 2).map { key -> ServiceFile(key) }

			val playbackPreparer = Mockito.mock(PlayableFilePreparationSource::class.java)
			Mockito.`when`(playbackPreparer.promisePreparedPlaybackFile(ServiceFile(0), Duration.ZERO))
				.thenReturn(Promise(FakePreparedPlayableFile(FakeBufferingPlaybackHandler())))
			Mockito.`when`(playbackPreparer.promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO))
				.thenReturn(Promise(Exception()))
				.thenReturn(Promise(FakePreparedPlayableFile(expectedPlaybackHandler)))

			val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
			val startPosition = 0
			val queue = PreparedPlayableFileQueue(
				{ 2 },
				playbackPreparer,
				bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition))

			try {
				returnedPlaybackHandler = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
					?.eventually { queue.promiseNextPreparedPlaybackFile(Duration.ZERO) }
					?.toFuture()?.get()?.playableFile
			} catch (e: ExecutionException) {
				error = e.cause
			}
		}
	}

	@Test
	fun thenTheExpectedPlaybackHandlerIsNotReturned() {
		Assertions.assertThat(returnedPlaybackHandler).isNotEqualTo(expectedPlaybackHandler).isNull()
	}

	@Test
	fun thenTheErrorIsCaught() {
		Assertions.assertThat(error).isNotNull
	}
}
