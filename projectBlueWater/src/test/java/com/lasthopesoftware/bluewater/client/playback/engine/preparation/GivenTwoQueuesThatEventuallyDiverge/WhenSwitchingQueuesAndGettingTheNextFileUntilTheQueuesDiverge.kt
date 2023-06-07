package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenTwoQueuesThatEventuallyDiverge

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.PositionedFileQueue
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSwitchingQueuesAndGettingTheNextFileUntilTheQueuesDiverge {
    private val expectedPositionedPlayableFile = PositionedPlayableFile(
		6,
		mockk(),
		NoTransformVolumeManager(),
		ServiceFile(6)
	)

	private val mut by lazy {
		val positionedFileQueue = mockk<PositionedFileQueue>().apply {
			every { libraryId } returns LibraryId(732)
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
			FakePlaybackQueueConfiguration(),
			{ l, _, _ ->
				queueLibraryIds.add(l)

				Promise(
					FakePreparedPlayableFile(
						FakeBufferingPlaybackHandler()
					)
				)
			},
			positionedFileQueue
		)

		val newPositionedFileQueue = mockk<PositionedFileQueue>().apply {
			every { libraryId } returns LibraryId(954)
			every { peek() } returns null
			every { poll() } returnsMany listOf(
				PositionedFile(3, ServiceFile(3)),
				PositionedFile(6, ServiceFile(6)),
				null
			)
		}

		Pair(queue, newPositionedFileQueue)
	}

	private val queueLibraryIds = mutableListOf<LibraryId>()
	private var positionedPlayableFile: PositionedPlayableFile? = null

	@BeforeAll
	fun act() {
		val (queue, newPositionedFileQueue) = mut
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
		queue.updateQueue(newPositionedFileQueue)
		queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
		positionedPlayableFile = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toExpiringFuture()?.get()
	}

	@Test
	fun `then the queue library ids are correct`() {
		assertThat(queueLibraryIds.map { it.id }.distinct()).containsExactly(732, 954)
	}

    @Test
    fun `then the queue continues`() {
        assertThat(positionedPlayableFile).isEqualTo(expectedPositionedPlayableFile)
    }
}
