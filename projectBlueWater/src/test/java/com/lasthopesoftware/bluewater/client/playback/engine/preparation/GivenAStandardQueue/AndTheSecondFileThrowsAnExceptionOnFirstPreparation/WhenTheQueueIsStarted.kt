package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndTheSecondFileThrowsAnExceptionOnFirstPreparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
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
import org.junit.jupiter.api.Test

private const val libraryId = 8

class WhenTheQueueIsStarted {

	private val expectedPlaybackHandler = FakeBufferingPlaybackHandler()
	private val returnedPlaybackHandler by lazy {
		val serviceFiles = (0..2).map(::ServiceFile)

		val playbackPreparer = mockk<PlayableFilePreparationSource>().apply {
			every { promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile(0), Duration.ZERO) } returns Promise(FakePreparedPlayableFile(FakeBufferingPlaybackHandler()))
			every { promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile(1), Duration.ZERO) } returnsMany listOf(
				Promise(Exception()),
				Promise(FakePreparedPlayableFile(expectedPlaybackHandler)),
			)
		}

		val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
		val startPosition = 0
		val queue = PreparedPlayableFileQueue(
			FakePlaybackQueueConfiguration(2),
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(LibraryId(libraryId), serviceFiles, startPosition))

		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			?.eventually { queue.promiseNextPreparedPlaybackFile(Duration.ZERO) }
			?.toExpiringFuture()
			?.get()
			?.playableFile
	}

	@Test
	fun `then the expected playback handler is returned`() {
		assertThat(returnedPlaybackHandler).isEqualTo(expectedPlaybackHandler).isNotNull
	}
}
