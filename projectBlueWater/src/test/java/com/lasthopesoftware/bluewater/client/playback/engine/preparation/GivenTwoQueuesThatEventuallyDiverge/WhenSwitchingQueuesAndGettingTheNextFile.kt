package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenTwoQueuesThatEventuallyDiverge

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSwitchingQueuesAndGettingTheNextFile {

	private val queue by lazy {
		val originalFileQueue = mockk<IPositionedFileQueue>().apply {
			every { poll() } returnsMany listOf(
				PositionedFile(1, ServiceFile(1)),
				PositionedFile(2, ServiceFile(2)),
				PositionedFile(3, ServiceFile(3)),
				PositionedFile(4, ServiceFile(4)),
				PositionedFile(5, ServiceFile(5)),
				null
			)
		}

		PreparedPlayableFileQueue(
			{ 3 },
			{ _, _ -> Promise<PreparedPlayableFile?>(FakePreparedPlayableFile(FakeBufferingPlaybackHandler())) },
			originalFileQueue)
	}
	private val playedFiles = ArrayList<PositionedPlayableFile>()
	private val expectedPositionedPlayableFile = listOf(
		PositionedPlayableFile(3, mockk(), NoTransformVolumeManager(), ServiceFile(3)),
		PositionedPlayableFile(4, mockk(), NoTransformVolumeManager(), ServiceFile(4)),
		PositionedPlayableFile(5, mockk(), NoTransformVolumeManager(), ServiceFile(6)),
		PositionedPlayableFile(6, mockk(), NoTransformVolumeManager(), ServiceFile(7)))

	@BeforeAll
	fun act() {

		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)

		val newPositionedFileQueue = mockk<IPositionedFileQueue>().apply {
			every { peek() } returns null
			every { poll() } returnsMany listOf(
				PositionedFile(3, ServiceFile(3)),
				PositionedFile(4, ServiceFile(4)),
				PositionedFile(5, ServiceFile(6)),
				PositionedFile(6, ServiceFile(7)),
				null
			)
		}
		queue.updateQueue(newPositionedFileQueue)
		queue
			.promiseNextPreparedPlaybackFile(Duration.ZERO)
			?.eventually { file ->
				playedFiles.add(file)
				queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			}
			?.eventually { file ->
				playedFiles.add(file)
				queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			}
			?.eventually { file ->
				playedFiles.add(file)
				queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			}
			?.then { file ->
				playedFiles.add(file)
			}
			?.toExpiringFuture()
			?.get()
	}

	@Test
	fun `then the queue continues to completion`() {
		assertThat(playedFiles).containsExactlyElementsOf(expectedPositionedPlayableFile)
	}
}
