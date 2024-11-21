package com.lasthopesoftware.bluewater.client.browsing

import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel

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
}
