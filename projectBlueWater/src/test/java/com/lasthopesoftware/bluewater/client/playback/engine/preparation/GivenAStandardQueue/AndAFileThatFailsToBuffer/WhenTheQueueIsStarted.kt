package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndAFileThatFailsToBuffer

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.Test
import java.io.IOException

class WhenTheQueueIsStarted {

	companion object {
		private val expectedPlaybackHandler = FakeBufferingPlaybackHandler()

		private val returnedPlaybackHandler by lazy {
			val serviceFiles = (0..2).map { key -> ServiceFile(key) }
			val playbackPreparer = mockk<PlayableFilePreparationSource>().apply {
				every { promisePreparedPlaybackFile(ServiceFile(0), Duration.ZERO) } returns Promise(
					FakePreparedPlayableFile<FakeBufferingPlaybackHandler>(object : FakeBufferingPlaybackHandler() {
						override fun promiseBufferedPlaybackFile(): Promise<IBufferingPlaybackFile> =
							Promise(IOException())
					})
				)

				every { promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO) } returns Promise(FakePreparedPlayableFile(expectedPlaybackHandler))
			}

			val startPosition = 0
			val queue = PreparedPlayableFileQueue(
				{ 2 },
				playbackPreparer,
				CompletingFileQueueProvider().provideQueue(serviceFiles, startPosition)
			)

			queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
				?.eventually { queue.promiseNextPreparedPlaybackFile(Duration.ZERO) }
				?.toExpiringFuture()
				?.get()
				?.playableFile
		}
	}

	@Test
	fun thenTheExpectedPlaybackHandlerIsReturned() {
		assertThat(returnedPlaybackHandler).isEqualTo(expectedPlaybackHandler)
	}
}
