package com.lasthopesoftware.bluewater.client.browsing

import com.lasthopesoftware.bluewater.client.browsing.files.details.BrowsedFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.NowPlayingFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.SearchedFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.search.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemsListViewModel
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.StoredFilesViewModel
import com.lasthopesoftware.bluewater.shared.android.UndoStack

/**
 * View Models that work best when declared with a local ViewModelOwner
 */
interface ScopedViewModelDependencies : ReusedViewModelDependencies {
	val itemListViewModel: ItemListViewModel
	val storedItemsListViewModel: StoredItemsListViewModel
	val fileListViewModel: FileListViewModel
	val itemDataLoader: LoadItemData
	val storedFilesViewModel: StoredFilesViewModel
	val searchFilesViewModel: SearchFilesViewModel
	val librarySettingsViewModel: LibrarySettingsViewModel
	val fileDetailsViewModel: FileDetailsViewModel
	val browsedFileDetailsViewModel: BrowsedFileDetailsViewModel
	val searchedFileDetailsViewModel: SearchedFileDetailsViewModel
	val nowPlayingFileDetailsViewModel: NowPlayingFileDetailsViewModel
	val undoBackStackBuilder: UndoStack
}
