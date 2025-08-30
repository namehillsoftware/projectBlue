package com.lasthopesoftware.bluewater.client.browsing.navigation

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
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.lasthopesoftware.bluewater.shared.android.viewmodels.ViewModelInitAction
import com.lasthopesoftware.promises.extensions.suspend
import java.io.IOException

@Composable
fun BrowserLibraryDestination.NavigateToLibraryDestination(browserViewDependencies: ScopedViewModelDependencies) {
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
					applicationNavigation = applicationNavigation,
					undoBackStack = undoBackStackBuilder,
				)

				activeFileDownloadsViewModel.loadActiveDownloads(libraryId)
			}
		}

		is SearchScreen -> {
			with(browserViewDependencies) {
				var isConnectionLost by remember { mutableStateOf(false) }
				var initializeConnection by remember { mutableStateOf(false) }

				if (isConnectionLost) {
					ConnectionLostView(
						onCancel = { applicationNavigation.viewApplicationSettings() },
						onRetry = {
							initializeConnection = true
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
					searchFilesViewModel.setActiveLibraryId(libraryId)

					if (initializeConnection) {
						LaunchedEffect(key1 = Unit) {
							isConnectionLost = !connectionStatusViewModel.initializeConnection(libraryId).suspend()
							initializeConnection = false
						}
					}

					if (!isConnectionLost) {
						LaunchedEffect(filePropertyFilter) {
							try {
								if (filePropertyFilter != null) {
									searchFilesViewModel.prependFilter(filePropertyFilter)
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
