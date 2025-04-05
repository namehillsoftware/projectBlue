package com.lasthopesoftware.bluewater.client.playback.file.preparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakePreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import kotlin.coroutines.cancellation.CancellationException

class FakeMappedPlayableFilePreparationSourceProvider(private val queue: List<ServiceFile>) : IPlayableFilePreparationSourceProvider {
	private var preparationSourceProvidedHandler: ((ServiceFile, DeferredPreparedPlayableFile) -> Unit)? = null
	val deferredResolutions = queue.associateWith { DeferredPreparedPlayableFile() }.toMutableMap()

	override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource {
        return PlayableFilePreparationSource { _, sf, preparedAt ->
			deferredResolutions[sf]
				?.also {
					it.preparedAt = preparedAt
					it.excuse { e ->
						if (e is ResetCancellationException) {
							deferredResolutions[sf] = DeferredPreparedPlayableFile()
						}
					}
					preparationSourceProvidedHandler?.invoke(sf, it)
				}
				.keepPromise()
        }
    }

    override val maxQueueSize = 1

	fun preparationSourceBeingProvided(handler: (ServiceFile, DeferredPreparedPlayableFile) -> Unit) {
		preparationSourceProvidedHandler = handler
	}

    class DeferredPreparedPlayableFile : Promise<PreparedPlayableFile>(), CancellationResponse {
		var preparedAt: Duration = Duration.ZERO

		init {
		    awaitCancellation(this)
		}

        fun resolve(): ResolvablePlaybackHandler {
			val playbackHandler = ResolvablePlaybackHandler()
			playbackHandler.setCurrentPosition(preparedAt.millis.toInt())
            resolve(FakePreparedPlayableFile(playbackHandler))
            return playbackHandler
        }

		override fun cancellationRequested() {
			reject(ResetCancellationException())
		}
	}

	private class ResetCancellationException : CancellationException()
}
