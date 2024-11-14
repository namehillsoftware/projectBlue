package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class MaxFileVolumePreparer(
	private val inner: PlayableFilePreparationSource,
	private val provideMaxFileVolume: ProvideMaxFileVolume
) : PlayableFilePreparationSource {
	override fun promisePreparedPlaybackFile(libraryId: LibraryId, serviceFile: ServiceFile, preparedAt: Duration): Promise<PreparedPlayableFile?> = Promise.Proxy { cp ->
		val promisedPreparedFile = inner.promisePreparedPlaybackFile(libraryId, serviceFile, preparedAt).also(cp::doCancel)
		provideMaxFileVolume
			.promiseMaxFileVolume(libraryId, serviceFile)
			.also(cp::doCancel)
			.eventually { maxFileVolume ->
				promisedPreparedFile
					.then { ppf ->
						ppf ?: return@then null

						val maxFileVolumeManager = MaxFileVolumeManager(ppf.playableFileVolumeManager)
						maxFileVolumeManager.setMaxFileVolume(maxFileVolume)

						PreparedPlayableFile(
							ppf.playbackHandler,
							maxFileVolumeManager,
							ppf.bufferingPlaybackFile
						)
					}
			}
	}
}
