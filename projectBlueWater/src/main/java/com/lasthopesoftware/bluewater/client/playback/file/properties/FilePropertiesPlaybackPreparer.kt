package com.lasthopesoftware.bluewater.client.playback.file.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ProvidePlayableFilePreparationSources
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration

class FilePropertiesPreparationProvider(
	private val preparationSourceProvider: ProvidePlayableFilePreparationSources,
	private val libraryFileProperties: ProvideLibraryFileProperties
) : ProvidePlayableFilePreparationSources by preparationSourceProvider {
	override fun providePlayableFilePreparationSource(): PlayableFilePreparationSource = FilePropertiesPlaybackPreparer(
		preparationSourceProvider.providePlayableFilePreparationSource())

	private inner class FilePropertiesPlaybackPreparer(
		private val innerPreparationSource: PlayableFilePreparationSource,
	) : PlayableFilePreparationSource {
		override fun promisePreparedPlaybackFile(
			libraryId: LibraryId,
			serviceFile: ServiceFile,
			preparedAt: Duration): Promise<PreparedPlayableFile?> =
			innerPreparationSource
				.promisePreparedPlaybackFile(libraryId, serviceFile, preparedAt)
				.cancelBackThen { file, _ ->
					file?.let { f ->
						PreparedPlayableFile(
							FilePropertiesDurationPlayableFile(
								f.playbackHandler,
								libraryFileProperties,
								libraryId,
								serviceFile
							),
							f.playableFileVolumeManager,
							f.bufferingPlaybackFile,
						)
					}
				}
	}
}
