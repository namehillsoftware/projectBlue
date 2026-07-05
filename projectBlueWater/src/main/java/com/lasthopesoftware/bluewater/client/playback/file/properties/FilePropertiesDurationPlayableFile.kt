package com.lasthopesoftware.bluewater.client.playback.file.properties

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.DurationFallbackPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class FilePropertiesDurationPlayableFile(
	private val playableFile: PlayableFile,
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val libraryId: LibraryId,
	private val serviceFile: ServiceFile,
) : PlayableFile by playableFile {
	override fun promisePlayback(): Promise<PlayingFile> =
		playableFile
			.promisePlayback()
			.cancelBackThen { file, _ ->
				DurationFallbackPlayingFile(
					file,
					FilePropertiesDurationReader(
						libraryFileProperties,
						libraryId,
						serviceFile,
					)
				)
			}
}

