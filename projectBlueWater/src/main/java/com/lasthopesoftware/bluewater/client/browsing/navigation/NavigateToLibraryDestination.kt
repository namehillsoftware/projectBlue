package com.lasthopesoftware.bluewater.client.browsing.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lasthopesoftware.bluewater.android.ui.ScreenDimensionsScope
import com.lasthopesoftware.bluewater.client.browsing.ScopedViewModelDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.list.search.SearchFilesView
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.items.list.LoadedItemListView
import com.lasthopesoftware.bluewater.client.browsing.items.list.LoadedItemListScreen
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsView
import com.lasthopesoftware.bluewater.shared.android.viewmodels.ViewModelInitAction
import com.lasthopesoftware.promises.extensions.suspend
import java.io.IOException

@Composable
fun ScreenDimensionsScope.NavigateToLibraryDestination(
	destination: BrowserLibraryDestination,
	browserViewDependencies: ScopedViewModelDependencies
) {
	when (destination) {
		is LibraryScreen -> {
			LoadedItemListScreen(browserViewDependencies, destination.libraryId, null)
		}

		is ItemScreen -> {
			LoadedItemListScreen(browserViewDependencies, destination.libraryId, destination.item)
		}

		is DownloadsScreen -> {
			with(browserViewDependencies) {
				ActiveFileDownloadsView(
					activeFileDownloadsViewModel = activeFileDownloadsViewModel,
					trackHeadlineViewModelProvider = reusableFileItemViewModelProvider,
					applicationNavigation = applicationNavigation,
                )

				DisposableEffect(destination) {
					val promise = activeFileDownloadsViewModel.loadActiveDownloads(destination.libraryId)

					onDispose {
						promise.cancel()
					}
				}
			}
		}

		is FilePropertySearchScreen, is SearchScreen -> {
			var isConnectionLost by remember { mutableStateOf(false) }
			var reinitializeConnection by remember { mutableStateOf(false) }

			browserViewDependencies.apply {
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
							isConnectionLost =
								!connectionStatusViewModel.initializeConnection(destination.libraryId).suspend()
							reinitializeConnection = false
						}
					}

					if (!isConnectionLost) {
						LaunchedEffect(destination) {
							try {
								when (destination) {
									is FilePropertySearchScreen -> {
										destination.filePropertyFilter?.let(searchFilesViewModel::prependFilter)
									}
									is SearchScreen -> {
										searchFilesViewModel.query.value = destination.searchQuery
									}
								}

								searchFilesViewModel.findFiles().suspend()
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
