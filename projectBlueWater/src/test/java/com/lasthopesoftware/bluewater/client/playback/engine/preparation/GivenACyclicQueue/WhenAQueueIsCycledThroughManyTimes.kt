package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenACyclicQueue

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt

class WhenAQueueIsCycledThroughManyTimes {

	private val mut by lazy {
		val numberOfFiles = nextInt(500)
		val serviceFiles = generateSequence { ServiceFile(nextInt()) }.take(numberOfFiles).toList()
		val fileActionMap = serviceFiles.associateWith { spyk(MockResolveAction()) }
		val bufferingPlaybackQueuesProvider = CyclicalFileQueueProvider()
		val queue = PreparedPlayableFileQueue(
			{ 1 },
			{ file, _ -> Promise(fileActionMap[file]) },
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, 0)
		)

		Pair(fileActionMap, queue)
	}
	private val expectedCycles by lazy { nextInt(100) }
	private val expectedNumberAbsolutePromises by lazy { expectedCycles * mut.first.size }
	private var returnedPromiseCount = 0

	@BeforeAll
	fun act() {
		val (_, queue) = mut

		for (i in 0 until expectedNumberAbsolutePromises) {
			val positionedPlaybackFilePromise = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
			if (positionedPlaybackFilePromise != null) ++returnedPromiseCount
		}
	}

    @Test
    fun `then each file is prepared the appropriate amount of times`() {
		val (fileActionMap) = mut
		assertThat(fileActionMap.values.map { it.preparedTimes }).allMatch { it == expectedCycles }
    }

    @Test
    fun `then the correct number of promises is returned`() {
		assertThat(returnedPromiseCount).isEqualTo(expectedNumberAbsolutePromises)
    }

    private class MockResolveAction : MessengerOperator<PreparedPlayableFile> {
		var preparedTimes = 0

        override fun send(messenger: Messenger<PreparedPlayableFile>) {
			++preparedTimes
            messenger.sendResolution(mockk())
        }
    }
}
