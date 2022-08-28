package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenTwoQueuesThatEventuallyDiverge

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test

class WhenSwitchingQueuesAndGettingTheNextFileUntilTheQueuesDiverge {
    private val expectedPositionedPlayableFile = PositionedPlayableFile(
		6,
		mockk(),
		NoTransformVolumeManager(),
		ServiceFile(6)
	)

	private val positionedPlayableFile by lazy {
		val positionedFileQueue = mockk<IPositionedFileQueue>().apply {
			every { poll() } returnsMany listOf(
				PositionedFile(1, ServiceFile(1)),
				PositionedFile(2, ServiceFile(2)),
				PositionedFile(3, ServiceFile(3)),
				PositionedFile(4, ServiceFile(4)),
				PositionedFile(5, ServiceFile(5)),
				null
			)
		}

		val queue = PreparedPlayableFileQueue(
			{ 1 },
			{ _, _ ->
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

		val newPositionedFileQueue = mockk<IPositionedFileQueue>().apply {
			every { peek() } returns null
			every { poll() } returnsMany listOf(
				PositionedFile(3, ServiceFile(3)),
				PositionedFile(6, ServiceFile(6)),
				null
			)
		}
		queue.updateQueue(newPositionedFileQueue)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toExpiringFuture()?.get()
	}

    @Test
    fun `then the queue continues`() {
        assertThat(positionedPlayableFile).isEqualTo(expectedPositionedPlayableFile)
    }
}
