package com.lasthopesoftware.bluewater.client.browsing

import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsFromItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsFromNowPlayingViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.ListedFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.search.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.shared.android.UndoStack

/**
 * View Models that work best when declared with a local ViewModelOwner
 */
interface ScopedViewModelDependencies : ReusedViewModelDependencies {
	val itemListViewModel: ItemListViewModel
	val fileListViewModel: FileListViewModel
	val activeFileDownloadsViewModel: ActiveFileDownloadsViewModel
	val searchFilesViewModel: SearchFilesViewModel
	val librarySettingsViewModel: LibrarySettingsViewModel
	val fileDetailsViewModel: FileDetailsViewModel
	val fileDetailsFromItemViewModel: FileDetailsFromItemViewModel
	val listedFileDetailsViewModel: ListedFileDetailsViewModel
	val fileDetailsFromNowPlayingViewModel: FileDetailsFromNowPlayingViewModel
	val undoBackStackBuilder: UndoStack
}
