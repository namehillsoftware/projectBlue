package com.lasthopesoftware.bluewater.client.playback.file.preparation

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class FakeDeferredPlayableFilePreparationSourceProvider : IPlayableFilePreparationSourceProvider {
	val deferredResolution = DeferredResolution()

    override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource {
        return PlayableFilePreparationSource { _, preparedAt ->
			deferredResolution.preparedAt = preparedAt
			Promise(deferredResolution)
        }
    }

    override fun getMaxQueueSize(): Int {
        return 1
    }

    class DeferredResolution : MessengerOperator<PreparedPlayableFile> {
        private var resolve: Messenger<PreparedPlayableFile>? = null

		var preparedAt: Duration = Duration.ZERO

        fun resolve(): ResolvablePlaybackHandler {
            val playbackHandler = ResolvablePlaybackHandler()
			playbackHandler.setCurrentPosition(preparedAt.millis.toInt())
            resolve?.sendResolution(FakePreparedPlayableFile(playbackHandler))
            return playbackHandler
        }

        override fun send(resolve: Messenger<PreparedPlayableFile>) {
            this.resolve = resolve
        }
    }
}
