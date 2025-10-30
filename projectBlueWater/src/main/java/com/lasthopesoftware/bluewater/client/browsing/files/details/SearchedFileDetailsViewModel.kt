package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.namehillsoftware.handoff.promises.Promise

class SearchedFileDetailsViewModel(
	playbackController: ControlPlaybackService,
	loadFileDetailsState: LoadFileDetailsState,
	fileDetailsState: FileDetailsState,
	private val itemFileProvider: ProvideLibraryFiles,
) : ListedFileDetailsViewModel<String>(
	playbackController,
	loadFileDetailsState,
	fileDetailsState,
) {
	override fun promiseFiles(libraryId: LibraryId, item: String): Promise<List<ServiceFile>> =
		itemFileProvider.promiseAudioFiles(libraryId, item)
}
