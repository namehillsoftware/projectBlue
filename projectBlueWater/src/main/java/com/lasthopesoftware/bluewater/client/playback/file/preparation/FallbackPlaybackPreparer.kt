package com.lasthopesoftware.bluewater.client.playback.file.preparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class FallbackPlaybackPreparer(
	private val primarySource: PlayableFilePreparationSource,
	private val fallbackSource: PlayableFilePreparationSource,
) : PlayableFilePreparationSource {
	companion object {
		private val logger by lazyLogger<FallbackPlaybackPreparer>()
	}

	override fun promisePreparedPlaybackFile(libraryId: LibraryId, serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?> = Promise.Proxy { cp ->
		primarySource
			.promisePreparedPlaybackFile(libraryId, serviceFile, preparedAt)
			.also(cp::doCancel)
			.eventually(forward()) { e ->
				if (cp.isCancelled) Promise(e)
				else {
					logger.error("An error occurred preparing file $serviceFile using the primary source, preparing using the fallback source.", e)
					fallbackSource.promisePreparedPlaybackFile(libraryId, serviceFile, preparedAt)
				}
			}
	}
}
