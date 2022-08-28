package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenACyclicQueue

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt

class WhenTheQueueIsStarted {

	private val mut by lazy {
		val numberOfFiles = nextInt(2, 500)
		val serviceFiles = (0..numberOfFiles).map { ServiceFile(nextInt()) }

		val fileActionMap = serviceFiles.associateBy ({ it }, { MockResolveAction() })
		val bufferingPlaybackQueuesProvider = CyclicalFileQueueProvider()
		val startPosition = nextInt(1, numberOfFiles)
		val queue = PreparedPlayableFileQueue(
			{ 1 },
			{ file, _ ->
				Promise(fileActionMap[file])
			},
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition)
		)
		Pair(startPosition, queue)
	}

	private var positionedPlaybackFile: PositionedPlayableFile? = null

	@BeforeAll
	fun act() {
		val (_, queue) = mut
		positionedPlaybackFile = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toExpiringFuture()?.get()
	}

	@Test
	fun `then the queue starts at the correct position`() {
		assertThat(positionedPlaybackFile?.playlistPosition).isEqualTo(mut.first)
	}

	private class MockResolveAction : MessengerOperator<PreparedPlayableFile> {
		override fun send(resolve: Messenger<PreparedPlayableFile?>) {
			resolve.sendResolution(mockk(relaxed = true))
		}
	}
}
