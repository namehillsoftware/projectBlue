package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import org.slf4j.LoggerFactory

class MaxFileVolumePreparer(
	private val playableFilePreparationSource: PlayableFilePreparationSource,
	private val provideMaxFileVolume: ProvideMaxFileVolume
) : PlayableFilePreparationSource {
	override fun promisePreparedPlaybackFile(libraryId: LibraryId, serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?> {
		val promisedMaxFileVolume = provideMaxFileVolume.promiseMaxFileVolume(libraryId, serviceFile)
		return playableFilePreparationSource
			.promisePreparedPlaybackFile(libraryId, serviceFile, preparedAt)
			.then { ppf ->
				ppf ?: return@then null

				val maxFileVolumeManager = MaxFileVolumeManager(ppf.playableFileVolumeManager)
				promisedMaxFileVolume
					.then(maxFileVolumeManager::setMaxFileVolume)
					.excuse { err ->
						logger.warn(
							"There was an error getting the max file volume for file $serviceFile",
							err
						)
					}

				PreparedPlayableFile(
					ppf.playbackHandler,
					maxFileVolumeManager,
					ppf.bufferingPlaybackFile
				)
			}
	}

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(MaxFileVolumePreparer::class.java) }
	}
}
