package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndTheSecondFileThrowsAnExceptionOnPreparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenTheQueueIsStarted {

	private val expectedPlaybackHandler = FakeBufferingPlaybackHandler()
	private val queue by lazy {
		val serviceFiles = (0..2).map(::ServiceFile)

		val playbackPreparer = mockk<PlayableFilePreparationSource>().apply {
			every { promisePreparedPlaybackFile(ServiceFile(0), Duration.ZERO) } returns Promise(FakePreparedPlayableFile(FakeBufferingPlaybackHandler()))
			every { promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO) } returns Promise(Exception())
		}

		val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
		val startPosition = 0
		PreparedPlayableFileQueue(
			{ 2 },
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition))
	}

	private var returnedPlaybackHandler: PlayableFile? = null
	private var error: Throwable? = null

	@BeforeAll
	fun act() {
		try {
			returnedPlaybackHandler = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
				?.eventually { queue.promiseNextPreparedPlaybackFile(Duration.ZERO) }
				?.toExpiringFuture()?.get()?.playableFile
		} catch (e: ExecutionException) {
			error = e.cause
		}
	}

	@Test
	fun `then the expected playback handler is not returned`() {
		assertThat(returnedPlaybackHandler).isNotEqualTo(expectedPlaybackHandler).isNull()
	}

	@Test
	fun `then the error is caught`() {
		assertThat(error).isNotNull
	}
}
