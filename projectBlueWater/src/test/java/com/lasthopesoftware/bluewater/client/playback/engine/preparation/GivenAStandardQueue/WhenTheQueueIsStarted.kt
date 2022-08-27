package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
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
	private val numberOfFiles by lazy { nextInt(500) }
	private val startPosition by lazy { nextInt(numberOfFiles) }
	private val queue by lazy {
		val serviceFiles = (0..numberOfFiles).map { ServiceFile(nextInt()) }
		val fileActionMap = serviceFiles.associateWith { MockResolveAction() }
		val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
		PreparedPlayableFileQueue(
			{ 1 },
			{ file: ServiceFile?, _: Duration? ->
				val mockResolveAction = fileActionMap[file]
				Promise(mockResolveAction)
			},
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition)
		)
	}

	private var preparedPlayableFile: PositionedPlayableFile? = null

	@BeforeAll
    fun act() {
		preparedPlayableFile = queue
			.promiseNextPreparedPlaybackFile(Duration.ZERO)
			?.toExpiringFuture()
			?.get()
    }

    @Test
    fun `then the queue starts at the correct position`() {
		assertThat(preparedPlayableFile?.playlistPosition).isEqualTo(startPosition)
    }

    private class MockResolveAction : MessengerOperator<PreparedPlayableFile?> {
        override fun send(resolve: Messenger<PreparedPlayableFile?>) {
            resolve.sendResolution(mockk(relaxed = true))
        }
    }
}
