package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenACyclicQueue

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt

class WhenAQueueIsCycledThroughManyTimes {

	private val mut by lazy {
		val numberOfFiles = nextInt(1, 500)
		val serviceFiles = generateSequence { ServiceFile(nextInt().toString()) }.take(numberOfFiles).toList()
		val fileActionMap = serviceFiles.associateWith { MockResolveAction() }
		val bufferingPlaybackQueuesProvider = CyclicalFileQueueProvider()
		val queue = PreparedPlayableFileQueue(
			FakePlaybackQueueConfiguration(),
			{ _, file, _ -> Promise(fileActionMap[file]) },
			bufferingPlaybackQueuesProvider.provideQueue(LibraryId(311), serviceFiles, 0)
		)

		Pair(fileActionMap, queue)
	}
	private val expectedCycles = 3
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
    fun `then each file except the first is prepared the appropriate amount of times`() {
		val (fileActionMap) = mut
		assertThat(fileActionMap.values.drop(1).map { it.preparedTimes }).allMatch { it == expectedCycles }
    }

	@Test
	fun `then the first is prepared the appropriate amount of times plus one because it will be buffered after the final cycle`() {
		val (fileActionMap) = mut
		assertThat(fileActionMap.values.first().preparedTimes).isEqualTo(expectedCycles + 1)
	}

    @Test
    fun `then the correct number of promises is returned`() {
		assertThat(returnedPromiseCount).isEqualTo(expectedNumberAbsolutePromises)
    }

    private class MockResolveAction : MessengerOperator<PreparedPlayableFile> {
		companion object {
			private val resolvedFile by lazy { FakePreparedPlayableFile(FakeBufferingPlaybackHandler()) }
		}

		var preparedTimes = 0

        override fun send(messenger: Messenger<PreparedPlayableFile>) {
			++preparedTimes
            messenger.sendResolution(resolvedFile)
        }
    }
}
