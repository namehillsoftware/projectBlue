package com.namehillsoftware.projectblue.tv.client.playback.nowplaying.view

import androidx.compose.runtime.Composable
import com.lasthopesoftware.bluewater.client.browsing.ScopedBrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ItemScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.namehillsoftware.projectblue.tv.client.browsing.items.list.LoadedTvItemListView
import com.namehillsoftware.projectblue.tv.client.browsing.items.list.TvSearchFilesView

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

                TvSearchFilesView(
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
