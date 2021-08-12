package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenACyclicQueue

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import kotlin.random.Random.Default.nextInt

class WhenTheQueueIsStarted {

    companion object {
		private var positionedPlaybackFile: PositionedPlayableFile? = null
        private var startPosition = 0

		@BeforeClass
		@JvmStatic
        fun before() {
            val numberOfFiles = nextInt(1, 500)
            val serviceFiles = (0..numberOfFiles).map { ServiceFile(nextInt()) }

            val fileActionMap = serviceFiles.associateBy ({ it }, { MockResolveAction() })
            val bufferingPlaybackQueuesProvider = CyclicalFileQueueProvider()
            startPosition = nextInt(1, numberOfFiles)
            val queue = PreparedPlayableFileQueue(
				{ 1 },
				{ file, _ ->
					Promise(fileActionMap[file])
				},
                bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition)
            )
			positionedPlaybackFile = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toFuture()?.get()
        }
    }

	@Test
	fun thenTheQueueStartsAtTheCorrectPosition() {
		assertThat(positionedPlaybackFile?.playlistPosition).isEqualTo(startPosition)
	}

	private class MockResolveAction : MessengerOperator<PreparedPlayableFile> {
		override fun send(resolve: Messenger<PreparedPlayableFile?>) {
			resolve.sendResolution(mockk(relaxed = true))
		}
	}
}
