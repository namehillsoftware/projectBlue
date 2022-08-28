package com.lasthopesoftware.bluewater.client.playback.engine.preparation.GivenAStandardQueue

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenTheQueueIsClosed {

	private val queue by lazy {
		val bufferingPlaybackQueuesProvider = CompletingFileQueueProvider()
		val cancelRecordingPromise = Promise { messenger: Messenger<PreparedPlayableFile?> ->
			messenger.cancellationRequested {
				isCancelled = true
			}
		}

		PreparedPlayableFileQueue(
			{ 1 },
			{ _, _ -> cancelRecordingPromise },
			bufferingPlaybackQueuesProvider.provideQueue(listOf(ServiceFile(1)), 0)
		)
	}

	private var isCancelled = false

    @BeforeAll
    fun before() {
        queue.promiseNextPreparedPlaybackFile(Duration.ZERO)
        queue.close()
    }

    @Test
    fun `then the prepared files are cancelled`() {
        assertThat(isCancelled).isTrue
    }
}
