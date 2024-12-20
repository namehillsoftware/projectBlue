package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledPlayButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledShuffleButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledActiveDownloadsButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledRefreshButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledSearchButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.MoreFileOptionsMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.MoreItemsOnlyOptionsMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.appBarHeight
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.expandedMenuVerticalPadding
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.expandedTitleHeight
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState

@Composable
fun TvItemListView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	childItemViewModelProvider: PooledCloseablesViewModel<ReusableChildItemViewModel>,
	applicationNavigation: NavigateApplication,
	playbackLibraryItems: PlaybackLibraryItems,
	playbackServiceController: ControlPlaybackService,
) {
	val files by fileListViewModel.files.subscribeAsState()
	val itemValue by itemListViewModel.itemValue.collectAsState()

	@Composable
	fun LoadedItemListView() {
		val items by itemListViewModel.items.collectAsState()

		LazyColumn(
			modifier = Modifier.focusGroup(),
			contentPadding = PaddingValues(Dimensions.viewPaddingUnit),
		) {
			if (items.any()) {
				item(contentType = ItemListContentType.Header) {
					ItemsCountHeader(items.size)
				}

				itemsIndexed(items, { _, i -> i.key }, { _, _ -> ItemListContentType.Item }) { i, f ->
					ChildItem(
						f,
						itemListViewModel,
						applicationNavigation,
						childItemViewModelProvider,
						itemListMenuBackPressedHandler,
						playbackLibraryItems
					)

					if (i < items.lastIndex)
						Divider()
				}
			}

			if (files.any()) {
				item(contentType = ItemListContentType.Header) {
					FilesCountHeader(files.size)
				}

				itemsIndexed(files, contentType = { _, _ -> ItemListContentType.File }) { i, f ->
					RenderTrackTitleItem(
						i,
						f,
						trackHeadlineViewModelProvider,
						itemListViewModel,
						nowPlayingViewModel,
						applicationNavigation,
						fileListViewModel,
						itemListMenuBackPressedHandler,
						playbackServiceController
					)

					if (i < files.lastIndex)
						Divider()
				}
			}
		}
	}

	val isFilesLoading by fileListViewModel.isLoading.subscribeAsState()

	ControlSurface {
		Column(modifier = Modifier.fillMaxSize()) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(appBarHeight),
				contentAlignment = Alignment.CenterStart,
			) {
				BackButton(
					applicationNavigation::navigateUp,
					modifier = Modifier
						.align(Alignment.TopStart)
						.padding(Dimensions.topRowOuterPadding)
				)

				if (files.any()) MoreFileOptionsMenu(fileListViewModel)
				else MoreItemsOnlyOptionsMenu(itemListViewModel, applicationNavigation)
			}

			Box(modifier = Modifier.height(expandedTitleHeight)) {
				ProvideTextStyle(MaterialTheme.typography.h5) {
					val startPadding = Dimensions.viewPaddingUnit
					val endPadding = Dimensions.viewPaddingUnit
					val maxLines = 2
					Text(
						text = itemValue,
						maxLines = maxLines,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier
							.fillMaxWidth()
							.padding(start = startPadding, end = endPadding),
					)
				}
			}

			if (!isFilesLoading) {
				Row(
					modifier = Modifier
						.padding(
							top = expandedMenuVerticalPadding,
							bottom = expandedMenuVerticalPadding,
							start = Dimensions.viewPaddingUnit * 2,
							end = Dimensions.viewPaddingUnit * 2
						)
						.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
				) {
					if (files.any()) {
						LabelledPlayButton(
							libraryState = itemListViewModel,
							playbackServiceController = playbackServiceController,
							serviceFilesListState = fileListViewModel,
						)

						LabelledShuffleButton(
							libraryState = itemListViewModel,
							playbackServiceController = playbackServiceController,
							serviceFilesListState = fileListViewModel,
						)

						LabelledRefreshButton(
							itemListViewModel = itemListViewModel,
							fileListViewModel = fileListViewModel,
						)
					} else {
						LabelledActiveDownloadsButton(
							itemListViewModel = itemListViewModel,
							applicationNavigation = applicationNavigation,
						)

						LabelledSearchButton(
							itemListViewModel = itemListViewModel,
							applicationNavigation = applicationNavigation,
						)

						LabelledRefreshButton(
							itemListViewModel = itemListViewModel,
							fileListViewModel = fileListViewModel,
						)
					}
				}
			}

			Box(modifier = Modifier.fillMaxSize()) {
				val isItemsLoading by itemListViewModel.isLoading.subscribeAsState()
				val isLoaded = !isItemsLoading && !isFilesLoading

				if (isLoaded) LoadedItemListView()
				else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}
		}
	}
}
