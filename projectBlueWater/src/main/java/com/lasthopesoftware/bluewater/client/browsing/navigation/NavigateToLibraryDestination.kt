package com.lasthopesoftware.bluewater.client.browsing.navigation

import LoadedItemListView
import LoadedTvItemListView
import androidx.compose.runtime.Composable
import com.lasthopesoftware.bluewater.client.browsing.ScopedBrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesView
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView

@Composable
fun BrowserLibraryDestination.NavigateToTvLibraryDestination(browserViewDependencies: ScopedBrowserViewDependencies) {
	when (this) {
		is LibraryScreen -> {
			LoadedTvItemListView(browserViewDependencies, libraryId, null)
		}

		is ItemScreen -> {
			LoadedTvItemListView(browserViewDependencies, libraryId, item)
		}

		is DownloadsScreen -> {
			with(browserViewDependencies) {
				ActiveFileDownloadsView(
					activeFileDownloadsViewModel = activeFileDownloadsViewModel,
					trackHeadlineViewModelProvider = reusableFileItemViewModelProvider,
					applicationNavigation,
				)

				activeFileDownloadsViewModel.loadActiveDownloads(libraryId)
			}
		}

		is SearchScreen -> {
			with(browserViewDependencies) {
				searchFilesViewModel.setActiveLibraryId(libraryId)

				SearchFilesView(
					searchFilesViewModel = searchFilesViewModel,
					nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
					trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					applicationNavigation = applicationNavigation,
					playbackServiceController = playbackServiceController,
				)
			}
		}
	}
}

@Composable
fun BrowserLibraryDestination.NavigateToLibraryDestination(browserViewDependencies: ScopedBrowserViewDependencies) {
	when (this) {
		is LibraryScreen -> {
			LoadedItemListView(browserViewDependencies, libraryId, null)
		}

		is ItemScreen -> {
			LoadedItemListView(browserViewDependencies, libraryId, item)
		}

		is DownloadsScreen -> {
			with(browserViewDependencies) {
				ActiveFileDownloadsView(
					activeFileDownloadsViewModel = activeFileDownloadsViewModel,
					trackHeadlineViewModelProvider = reusableFileItemViewModelProvider,
					applicationNavigation,
				)

				activeFileDownloadsViewModel.loadActiveDownloads(libraryId)
			}
		}

		is SearchScreen -> {
			with(browserViewDependencies) {
				searchFilesViewModel.setActiveLibraryId(libraryId)

				SearchFilesView(
					searchFilesViewModel = searchFilesViewModel,
					nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
					trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					applicationNavigation = applicationNavigation,
					playbackServiceController = playbackServiceController,
				)
			}
		}
	}
}
