package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import LoadedItemListView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.list.search.SearchFilesView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.navigation.BrowserLibraryDestination
import com.lasthopesoftware.bluewater.client.browsing.navigation.DownloadsScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.ItemScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.LibraryScreen
import com.lasthopesoftware.bluewater.client.browsing.navigation.SearchScreen
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.lasthopesoftware.bluewater.shared.android.viewmodels.ViewModelInitAction
import com.lasthopesoftware.promises.extensions.suspend
import java.io.IOException

@Composable
fun ScreenDimensionsScope.NavigateToTvLibraryDestination(destination: BrowserLibraryDestination, browserViewDependencies: ScopedViewModelDependencies) {
	when (destination) {
		is LibraryScreen -> {
            LoadedItemListView(browserViewDependencies, destination.libraryId, null)
		}

		is ItemScreen -> {
            LoadedItemListView(browserViewDependencies, destination.libraryId, destination.item)
		}

		is DownloadsScreen -> {
			with(browserViewDependencies) {
				ActiveFileDownloadsView(
                    activeFileDownloadsViewModel = activeFileDownloadsViewModel,
                    trackHeadlineViewModelProvider = reusableFileItemViewModelProvider,
					applicationNavigation = applicationNavigation,
                )

				activeFileDownloadsViewModel.loadActiveDownloads(destination.libraryId)
			}
		}

		is SearchScreen -> {
			with(browserViewDependencies) {
				var isConnectionLost by remember { mutableStateOf(false) }
				var reinitializeConnection by remember { mutableStateOf(false) }

				if (isConnectionLost) {
					ConnectionLostView(
						onCancel = { applicationNavigation.viewApplicationSettings() },
						onRetry = {
							reinitializeConnection = true
						}
					)
				} else {
					SearchFilesView(
						searchFilesViewModel = searchFilesViewModel,
						nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
						trackHeadlineViewModelProvider = reusablePlaylistFileItemViewModelProvider,
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
						applicationNavigation = applicationNavigation,
						playbackServiceController = playbackServiceController,
						stringResources = stringResources,
						backStackBuilder = undoBackStackBuilder,
					)
				}

				ViewModelInitAction {
					searchFilesViewModel.setActiveLibraryId(destination.libraryId)

					if (reinitializeConnection) {
						LaunchedEffect(key1 = Unit) {
							isConnectionLost = !connectionStatusViewModel.initializeConnection(destination.libraryId).suspend()
							reinitializeConnection = false
						}
					}

					if (!isConnectionLost) {
						LaunchedEffect(destination.filePropertyFilter) {
							try {
								if (destination.filePropertyFilter != null) {
									searchFilesViewModel.prependFilter(destination.filePropertyFilter)
									searchFilesViewModel.findFiles().suspend()
								}
							} catch (e: IOException) {
								if (ConnectionLostExceptionFilter.isConnectionLostException(e))
									isConnectionLost = true
								else
									applicationNavigation.backOut().suspend()
							} catch (_: Exception) {
								applicationNavigation.backOut().suspend()
							}
						}
					}
				}
			}
		}
	}
}
