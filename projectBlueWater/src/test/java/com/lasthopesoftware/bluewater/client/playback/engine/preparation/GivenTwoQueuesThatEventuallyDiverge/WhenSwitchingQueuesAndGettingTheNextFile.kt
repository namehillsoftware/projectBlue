package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenTwoQueuesThatEventuallyDiverge

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class WhenSwitchingQueuesAndGettingTheNextFile {

	companion object {
		private val playedFiles: MutableList<PositionedPlayableFile> = ArrayList()
		private val expectedPositionedPlayableFile = listOf(
			PositionedPlayableFile(3, Mockito.mock(PlayableFile::class.java), NoTransformVolumeManager(), ServiceFile(3)),
			PositionedPlayableFile(4, Mockito.mock(PlayableFile::class.java), NoTransformVolumeManager(), ServiceFile(4)),
			PositionedPlayableFile(5, Mockito.mock(PlayableFile::class.java), NoTransformVolumeManager(), ServiceFile(6)),
			PositionedPlayableFile(6, Mockito.mock(PlayableFile::class.java), NoTransformVolumeManager(), ServiceFile(7)))

		@JvmStatic
		@BeforeClass
		fun before() {
			val positionedFileQueue = Mockito.mock(IPositionedFileQueue::class.java)
			Mockito.`when`(positionedFileQueue.poll())
				.thenReturn(
					PositionedFile(1, ServiceFile(1)),
					PositionedFile(2, ServiceFile(2)),
					PositionedFile(3, ServiceFile(3)),
					PositionedFile(4, ServiceFile(4)),
					PositionedFile(5, ServiceFile(5)),
					null)
			val queue = PreparedPlayableFileQueue(
				{ 3 },
				{ _, _ -> Promise<PreparedPlayableFile>(FakePreparedPlayableFile(FakeBufferingPlaybackHandler())) },
				positionedFileQueue)
			queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			val newPositionedFileQueue = Mockito.mock(IPositionedFileQueue::class.java)
			Mockito.`when`(newPositionedFileQueue.poll())
				.thenReturn(
					PositionedFile(3, ServiceFile(3)),
					PositionedFile(4, ServiceFile(4)),
					PositionedFile(5, ServiceFile(6)),
					PositionedFile(6, ServiceFile(7)),
					null)
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
		}
	}

	@Test
	fun thenTheQueueContinuesToCompletion() {
		Assertions.assertThat(playedFiles).asList().containsExactlyElementsOf(expectedPositionedPlayableFile)
	}
}
