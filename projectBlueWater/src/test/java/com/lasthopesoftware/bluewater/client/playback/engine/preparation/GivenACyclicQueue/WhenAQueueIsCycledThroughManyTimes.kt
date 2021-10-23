package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenACyclicQueue

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
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
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenAQueueIsCycledThroughManyTimes {

	companion object {
		private var fileActionMap: Map<ServiceFile, MockResolveAction>? = null
		private var expectedNumberAbsolutePromises = 0
		private var expectedCycles = 0
		private var returnedPromiseCount = 0

		@BeforeClass
		@JvmStatic
		fun before() {
			val random = Random()
			val numberOfFiles = random.nextInt(500)
			val serviceFiles = generateSequence { ServiceFile(random.nextInt()) }.take(numberOfFiles).toList()
			fileActionMap = serviceFiles.associateWith { spyk(MockResolveAction()) }
			val bufferingPlaybackQueuesProvider = CyclicalFileQueueProvider()
			val queue = PreparedPlayableFileQueue(
				{ 1 },
				{ file, _ -> Promise(fileActionMap!![file]) },
				bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, 0)
			)
			expectedCycles = random.nextInt(100)
			expectedNumberAbsolutePromises = expectedCycles * numberOfFiles
			for (i in 0 until expectedNumberAbsolutePromises) {
				val positionedPlaybackFilePromise = queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
				if (positionedPlaybackFilePromise != null) ++returnedPromiseCount
			}
		}
	}

    @Test
    fun thenEachFileIsPreparedTheAppropriateAmountOfTimes() {
		fileActionMap!!.values.forEach { v -> assertThat(v.preparedTimes).isEqualTo(expectedCycles) }
    }

    @Test
    fun thenTheCorrectNumberOfPromisesIsReturned() {
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
