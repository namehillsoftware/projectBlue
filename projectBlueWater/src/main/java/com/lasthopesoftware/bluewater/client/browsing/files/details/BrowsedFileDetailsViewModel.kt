package com.lasthopesoftware.bluewater.client.browsing.files.details

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class BrowsedFileDetailsViewModel(
	private val playbackController: ControlPlaybackService,
	private val loadFileDetailsState: LoadFileDetailsState,
	private val fileDetailsState: FileDetailsState,
	private val itemFileProvider: ProvideLibraryFiles,
) : ViewModel(), PlayableFileDetailsState, FileDetailsState by fileDetailsState {
	private var activePosition: Int? = null
	private var activeFiles: List<ServiceFile>? = null

	fun load(libraryId: LibraryId, item: IItem?, positionedFile: PositionedFile): Promise<Unit> {
		val (position, serviceFile) = positionedFile

		val promisedLibraryFiles = when (item) {
			is Item -> itemFileProvider.promiseFiles(libraryId, ItemId(item.key))
			is Playlist -> itemFileProvider.promiseFiles(libraryId, item.itemId)
			else -> itemFileProvider.promiseFiles(libraryId)
		}.cancelBackThen { f, cs ->
			activeFiles = f

			if (!cs.isCancelled) {
				activePosition = f.elementAtOrNull(position)
					?.takeIf { it == serviceFile }
					?.let { position }
			}
		}

		val promisedFileDetails = loadFileDetailsState.load(libraryId, positionedFile.serviceFile)

		return Promise.whenAll(promisedLibraryFiles, promisedFileDetails).unitResponse()
	}

	override fun play() {
		playbackController.startPlaylist(
			libraryId = activeLibraryId ?: return,
			serviceFiles = activeFiles ?: return,
			position = activePosition ?: return,
		)
	}
}
