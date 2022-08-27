package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt

class WhenTheQueueIsConsumed {

	private val expectedNumberOfFiles by lazy { nextInt(1, 501) }

	private val mut by lazy {
		val serviceFiles = (0 until expectedNumberOfFiles).map { ServiceFile(nextInt()) }
		val fileActionMap = serviceFiles.associateWith { MockResolveAction() }
		val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
		val queue = PreparedPlayableFileQueue(
			{ 1 },
			{ file, _ -> Promise(fileActionMap[file]) },
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, 0)
		)
		Pair(fileActionMap, queue)
	}

	private val fileActionMap: Map<ServiceFile, MockResolveAction>
		get() = mut.first

	private var returnedPromiseCount = 0

	@BeforeAll
	fun before() {
		val (_, queue) = mut
		val expectedCycles = nextInt(1, 101)
		val expectedNumberAbsolutePromises = expectedCycles * expectedNumberOfFiles
		for (i in 0 until expectedNumberAbsolutePromises) {
			val positionedPlaybackFilePromise = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			if (positionedPlaybackFilePromise != null) ++returnedPromiseCount
		}
	}

	@Test
	fun `then each file is prepared the appropriate amount of times`() {
		assertThat(
			fileActionMap
				.map { (_, value) -> value.calls }
				.distinct()
				.single()).isEqualTo(1)
	}

	@Test
	fun `then the correct number of promises is returned`() {
		assertThat(returnedPromiseCount).isEqualTo(expectedNumberOfFiles)
	}

	private class MockResolveAction : MessengerOperator<PreparedPlayableFile?> {
		var calls = 0
		override fun send(resolve: Messenger<PreparedPlayableFile?>) {
			++calls
			resolve.sendResolution(mockk())
		}
	}
}
