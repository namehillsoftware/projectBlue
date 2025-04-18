package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenTwoQueuesThatEventuallyDiverge

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.PositionedFileQueue
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test

class WhenSwitchingQueuesAndTheNextQueueIsEmpty {
	private val nextPreparedPlaybackFilePromise by lazy {
		val positionedFileQueue = mockk<PositionedFileQueue>().apply {
			every { libraryId } returns LibraryId(168)
			every { poll() } returnsMany listOf(
				PositionedFile(1, ServiceFile("1")),
				PositionedFile(2, ServiceFile("2")),
				PositionedFile(3, ServiceFile("3")),
				PositionedFile(4, ServiceFile("4")),
				PositionedFile(5, ServiceFile("5")),
				null
			)
		}

		val queue = PreparedPlayableFileQueue(
			FakePlaybackQueueConfiguration(),
			{ _, _, _ ->
				Promise(
					FakePreparedPlayableFile(
						FakeBufferingPlaybackHandler()
					)
				)
			},
			positionedFileQueue
		)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
		val newPositionedFileQueue = mockk<PositionedFileQueue>().apply {
			every { libraryId } returns LibraryId(39)
			every { peek() } returns null
			every { poll() } returns null
		}
		queue.updateQueue(newPositionedFileQueue)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
	}

    @Test
    fun `then the queue does not continue`() {
        assertThat(nextPreparedPlaybackFilePromise).isNull()
    }
}
