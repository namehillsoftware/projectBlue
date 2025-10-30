package com.lasthopesoftware.bluewater.client.browsing.files.details

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.namehillsoftware.handoff.promises.Promise

class BrowsedFileDetailsViewModel(
	playbackController: ControlPlaybackService,
	loadFileDetailsState: LoadFileDetailsState,
	fileDetailsState: FileDetailsState,
	private val itemFileProvider: ProvideLibraryFiles,
) : ListedFileDetailsViewModel<IItem?>(
	playbackController,
	loadFileDetailsState,
	fileDetailsState,
) {
	override fun promiseFiles(libraryId: LibraryId, item: IItem?): Promise<List<ServiceFile>> = when (item) {
		is Item -> itemFileProvider.promiseFiles(libraryId, ItemId(item.key))
		is Playlist -> itemFileProvider.promiseFiles(libraryId, item.itemId)
		else -> itemFileProvider.promiseFiles(libraryId)
	}
}
