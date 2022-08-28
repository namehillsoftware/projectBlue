package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue.AndAFileThatFailsToPrepare

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
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

	private val mut by lazy {
		val serviceFiles = (0..2).map { key -> ServiceFile(key) }
		val playbackPreparer = mockk<PlayableFilePreparationSource>().apply {
			every { promisePreparedPlaybackFile(ServiceFile(0), Duration.ZERO) } returns Promise(expectedException)
			every { promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO) } returns Promise(
				FakePreparedPlayableFile(FakeBufferingPlaybackHandler())
			)
		}

		val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
		val startPosition = 0
		PreparedPlayableFileQueue(
			{ 2 },
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition)
		)
	}
	private val expectedException = Exception()
	private var caughtException: PreparationException? = null
	private var returnedPlaybackHandler: PlayableFile? = null

	@BeforeAll
	fun before() {

		try {
			mut.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toExpiringFuture()?.get()
			returnedPlaybackHandler = mut.promiseNextPreparedPlaybackFile(Duration.ZERO)?.toExpiringFuture()?.get()?.playableFile
		} catch (ee: ExecutionException) {
			val cause = ee.cause
			if (cause is PreparationException) caughtException = cause
		}
	}

	@Test
	fun `then the positioned file exception is caught`() {
		assertThat(caughtException).hasCause(expectedException)
	}

	@Test
	fun `then the positioned file exception contains the positioned file`() {
		assertThat(caughtException!!.positionedFile)
			.isEqualTo(PositionedFile(0, ServiceFile(0)))
	}

	@Test
	fun `then the expected playback handler is not returned`() {
		assertThat(returnedPlaybackHandler).isNull()
	}
}
