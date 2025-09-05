package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.AnchoredChips
import com.lasthopesoftware.bluewater.android.ui.components.AnchoredProgressScrollConnectionDispatcher
import com.lasthopesoftware.bluewater.android.ui.components.AnchoredScrollConnectionState
import com.lasthopesoftware.bluewater.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.android.ui.components.ConsumedOffsetErasingNestedScrollConnection
import com.lasthopesoftware.bluewater.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.android.ui.components.LinkedNestedScrollConnection
import com.lasthopesoftware.bluewater.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.android.ui.components.rememberAnchoredScrollConnectionState
import com.lasthopesoftware.bluewater.android.ui.components.rememberFullScreenScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.rememberPreScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.rememberTitleStartPadding
import com.lasthopesoftware.bluewater.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.rememberAutoCloseable
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.DetermineWindowControlColors
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.appBarHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.expandedTitleHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowScrollPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.standardRowHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconSize
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topRowOuterPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledPlayButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledShuffleButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledSyncButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackTitleItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledActiveDownloadsButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledRefreshButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledSearchButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledSettingsButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.ScreenDimensionsScope
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.minimumMenuWidth
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.UndoStack
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.GetStringResources
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlin.math.roundToInt

private val appBarAndTitleHeight = expandedTitleHeight + appBarHeight

@Composable
fun ItemsCountHeader(itemsCount: Int) {
	Box(
		modifier = Modifier
			.height(topMenuHeight)
			.padding(viewPaddingUnit)
	) {
		ProvideTextStyle(MaterialTheme.typography.h5) {
			Text(
				text = stringResource(R.string.item_count_label, itemsCount),
				fontWeight = FontWeight.Bold,
				modifier = Modifier
					.padding(viewPaddingUnit)
					.align(Alignment.CenterStart)
			)
		}
	}
}

@Composable
fun FilesCountHeader(filesCount: Int) {
	Box(
		modifier = Modifier
			.padding(viewPaddingUnit)
			.height(topMenuHeight)
	) {
		ProvideTextStyle(MaterialTheme.typography.h5) {
			Text(
				text = stringResource(R.string.file_count_label, filesCount),
				fontWeight = FontWeight.Bold,
				modifier = Modifier
					.padding(viewPaddingUnit)
					.align(Alignment.CenterStart)
			)
		}
	}
}

@Composable
fun RenderTrackTitleItem(
    position: Int,
    serviceFile: ServiceFile,
    trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
    itemListViewModel: ItemListViewModel,
    nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
    applicationNavigation: NavigateApplication,
    fileListViewModel: FileListViewModel,
    itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
    playbackServiceController: ControlPlaybackService,
    undoBackStack: UndoStack,
) {
	val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

	DisposableEffect(serviceFile) {
		itemListViewModel.loadedLibraryId?.also {
			fileItemViewModel.promiseUpdate(it, serviceFile)
		}

		onDispose {
			fileItemViewModel.reset()
		}
	}

	val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
	val fileName by fileItemViewModel.title.collectAsState()
	val playingFile by nowPlayingViewModel.nowPlayingFile.subscribeAsState()
	val isPlaying by remember(serviceFile) { derivedStateOf { playingFile?.serviceFile == serviceFile } }

	val viewFileDetailsClickHandler = remember(position) {
		{
			itemListViewModel.loadedLibraryId?.also {
				itemListViewModel.loadedItem?.also { item ->
					applicationNavigation
						.viewFileDetails(it, fileListViewModel.files.value, position)
				}
			}
			Unit
		}
	}

	TrackTitleItemView(
		itemName = fileName,
		isActive = isPlaying,
		isHiddenMenuShown = isMenuShown,
		onItemClick = viewFileDetailsClickHandler,
		onHiddenMenuClick = {
			itemListMenuBackPressedHandler.hideAllMenus()
			fileItemViewModel.showMenu()

			undoBackStack.addAction {
				fileItemViewModel.hideMenu().toPromise()
			}
		},
		onAddToNowPlayingClick = {
			itemListViewModel.loadedLibraryId?.also {
				playbackServiceController.addToPlaylist(it, serviceFile)
			}
		},
		onViewFilesClick = viewFileDetailsClickHandler,
		onPlayClick = {
			fileItemViewModel.hideMenu()
			itemListViewModel.loadedLibraryId?.also {
				playbackServiceController.startPlaylist(it, fileListViewModel.files.value, position)
			}
		}
	)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChildItem(
    item: IItem,
    itemListViewModel: ItemListViewModel,
    applicationNavigation: NavigateApplication,
    childItemViewModelProvider: PooledCloseablesViewModel<ReusableChildItemViewModel>,
    itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
    playbackLibraryItems: PlaybackLibraryItems,
    backStack: UndoStack,
) {
	val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }

	val childItemViewModel = remember(childItemViewModelProvider::getViewModel)

	DisposableEffect(key1 = item) {
		itemListViewModel.loadedLibraryId?.also {
			childItemViewModel.update(it, item)
		}

		onDispose {
			childItemViewModel.reset()
		}
	}

	val isMenuShown by childItemViewModel.isMenuShown.collectAsState()
	val hapticFeedback = LocalHapticFeedback.current

	if (!isMenuShown) {
		Box(modifier = Modifier
			.navigable(
				interactionSource = remember { MutableInteractionSource() }, indication = null, onLongClick = {
					hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

					itemListMenuBackPressedHandler.hideAllMenus()

					childItemViewModel.showMenu()

					backStack.addAction {
						childItemViewModel.hideMenu().toPromise()
					}
				}, onClickLabel = stringResource(id = R.string.btn_view_song_details), onClick = {
					itemListViewModel.loadedLibraryId?.also {
						applicationNavigation.viewItem(it, item)
					}
				}, scrollPadding = standardRowHeight.rowScrollPadding
			)
			.height(standardRowHeight)
			.fillMaxSize()
			.padding(Dimensions.rowPaddingValues)
		) {
			Text(
				text = item.value ?: "",
				fontSize = rowFontSize,
				overflow = TextOverflow.Ellipsis,
				maxLines = 1,
				fontWeight = FontWeight.Normal,
				modifier = Modifier
					.align(Alignment.CenterStart),
			)
		}
	} else {
		Row(
			modifier = Modifier
				.height(standardRowHeight)
				.padding(Dimensions.rowPaddingValues)
		) {
			ListItemIcon(
				painter = painterResource(id = R.drawable.av_play),
				contentDescription = stringResource(id = R.string.btn_play),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.navigable(
						onClick = {
							itemListViewModel.loadedLibraryId?.also {
								when (item) {
									is Item -> playbackLibraryItems.playItem(it, ItemId(item.key))
									is Playlist -> playbackLibraryItems.playPlaylist(it, PlaylistId(item.key))
								}
							}
						},
						isDefault = true,
					)
					.align(Alignment.CenterVertically),
			)

			val isChildItemSynced by childItemViewModel.isSynced.collectAsState()
			SyncIcon(
				isActive = isChildItemSynced,
				modifier = Modifier
					.fillMaxWidth()
					.navigable(onClick = childItemViewModel::toggleSync)
					.weight(1f)
					.align(Alignment.CenterVertically),
			)

			ListItemIcon(
				painter = painterResource(id = R.drawable.av_shuffle),
				contentDescription = stringResource(id = R.string.btn_shuffle_files),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.navigable(onClick = {
						itemListViewModel.loadedLibraryId?.also {
							when (item) {
								is Item -> playbackLibraryItems.playItemShuffled(it, ItemId(item.key))
								is Playlist -> playbackLibraryItems.playPlaylistShuffled(it, PlaylistId(item.key))
							}
						}
					})
					.align(Alignment.CenterVertically),
			)
		}
	}
}

@Composable
fun ItemListMenu(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	itemDataLoader: LoadItemData,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = Modifier
			.height(topMenuHeight)
			.padding(vertical = rowPadding)
			.focusGroup()
			.then(modifier)
			.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.Start,
	) {
		val menuIconModifier = Modifier.width(topMenuIconSize * 4)

		val isFilesLoading by fileListViewModel.isLoading.subscribeAsState()
		val isItemsLoading by itemListViewModel.isLoading.subscribeAsState()
		val isNotLoading by remember { derivedStateOf { !isFilesLoading && !isItemsLoading } }
		if (isNotLoading) {
			LabelledRefreshButton(
				itemDataLoader = itemDataLoader,
				modifier = menuIconModifier,
			)
		}

		val files by fileListViewModel.files.subscribeAsState()
		if (files.any()) {
			LabelledPlayButton(
				libraryState = itemListViewModel,
				playbackServiceController = playbackServiceController,
				serviceFilesListState = fileListViewModel,
				modifier = menuIconModifier,
			)

			LabelledShuffleButton(
				libraryState = itemListViewModel,
				playbackServiceController = playbackServiceController,
				serviceFilesListState = fileListViewModel,
				modifier = menuIconModifier,
			)

			LabelledSyncButton(
				fileListViewModel,
				modifier = menuIconModifier,
			)
		}

		LabelledActiveDownloadsButton(
			loadedLibraryState = itemListViewModel,
			applicationNavigation = applicationNavigation,
			modifier = menuIconModifier,
		)

		LabelledSettingsButton(
			itemListViewModel,
			applicationNavigation,
			modifier = menuIconModifier,
		)

		LabelledSearchButton(
			itemListViewModel = itemListViewModel,
			applicationNavigation = applicationNavigation,
			modifier = menuIconModifier,
		)
	}
}

@Composable
fun ItemListView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	itemDataLoader: LoadItemData,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	childItemViewModelProvider: PooledCloseablesViewModel<ReusableChildItemViewModel>,
	applicationNavigation: NavigateApplication,
	playbackLibraryItems: PlaybackLibraryItems,
	playbackServiceController: ControlPlaybackService,
	undoBackStack: UndoStack,
	lazyListState: LazyListState,
	anchoredScrollConnectionState: AnchoredScrollConnectionState,
	chipLabel: @Composable (Int, Float) -> Unit,
	onScrollProgress: (Float) -> Unit,
	headerHeight: Dp = 0.dp,
) {
	BoxWithConstraints(modifier = Modifier
		.fillMaxSize()
		) {
		val isLoading by itemDataLoader.isLoading.subscribeAsState()
		if (isLoading) {
			CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
		} else {
			val items by itemListViewModel.items.subscribeAsState()
			val files by fileListViewModel.files.subscribeAsState()

			LaunchedEffect(anchoredScrollConnectionState) {
				snapshotFlow { anchoredScrollConnectionState.selectedProgress }
					.drop(1) // Ignore initial state
					.map {
						val layoutInfo = lazyListState.layoutInfo
						it?.let { progress ->
							val totalItems = layoutInfo.totalItemsCount

							val newIndex = (totalItems - 1) * progress
							newIndex
						}
					}
					.distinctUntilChanged()
					.collect {
						it?.let { fractionalIndex ->
							if (DebugFlag.isDebugCompilation) {
								Log.d("LoadedItemListView", "Selected index: $it")
							}

							val index = fractionalIndex.toInt()

							lazyListState.scrollToItem(index)

							val offsetPercentage = fractionalIndex - index
							if (offsetPercentage != 0f) {
								val offsetPixels = lazyListState
									.layoutInfo
									.visibleItemsInfo
									.firstOrNull()
									?.size
									?.let { s -> s * offsetPercentage }
									?.takeIf { p -> p != 0f }

								if (offsetPixels != null) {
									lazyListState.scrollBy(offsetPixels)
								}
							}
						}
					}
			}

			LazyColumn(
				state = lazyListState,
				contentPadding = PaddingValues(top = headerHeight),
				modifier = Modifier.focusGroup(),
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
							playbackLibraryItems,
							undoBackStack,
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
							playbackServiceController,
							undoBackStack,
						)

						if (i < files.lastIndex)
							Divider()
					}
				}
			}


			if (LocalInputModeManager.current.inputMode == InputMode.Touch) {        // 5in in pixels, pixels/Inch
				val maxScrollBarHeight = remember( maxHeight, headerHeight) {
					val dpi = 160f
					(2.5f * dpi).dp.coerceAtMost(maxHeight - headerHeight - topMenuHeight)
				}

				val localHapticFeedback = LocalHapticFeedback.current
				AnchoredChips(
					modifier = Modifier
						.heightIn(200.dp, maxScrollBarHeight)
						.align(Alignment.BottomEnd),
					anchoredScrollConnectionState = anchoredScrollConnectionState,
					lazyListState = lazyListState,
					chipLabel = chipLabel,
					onScrollProgress = onScrollProgress,
					onSelected = {
						localHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
					}
				)
			}
		}
	}
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ScreenDimensionsScope.ItemListView(
    itemListViewModel: ItemListViewModel,
    fileListViewModel: FileListViewModel,
	itemDataLoader: LoadItemData,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
    itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
    trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
    childItemViewModelProvider: PooledCloseablesViewModel<ReusableChildItemViewModel>,
    applicationNavigation: NavigateApplication,
    playbackLibraryItems: PlaybackLibraryItems,
    playbackServiceController: ControlPlaybackService,
	stringResources: GetStringResources,
    undoBackStack: UndoStack,
) {
	val itemValue by itemListViewModel.itemValue.subscribeAsState()

	val lazyListState = rememberLazyListState()

	ControlSurface {
		DetermineWindowControlColors()

		val collapsedHeight = appBarHeight

		var minVisibleItemsForScroll by remember { mutableIntStateOf(30) }
		LaunchedEffect(lazyListState, itemDataLoader) {
			combine(
				snapshotFlow { lazyListState.layoutInfo },
				itemDataLoader.isLoading.mapNotNull().asFlow(),
			) { info, i -> Pair(info, i) }
				.filterNot { (_, i) -> i }
				.map { (info, _) -> info.visibleItemsInfo.size }
				.filter { it == 0 }
				.distinctUntilChanged()
				.take(1)
				.collect {
					minVisibleItemsForScroll = (it * 3).coerceAtLeast(30)
				}
		}

		val items by itemListViewModel.items.subscribeAsState()
		val files by fileListViewModel.files.subscribeAsState()
		val labeledAnchors by remember {
			derivedStateOf {
				var totalItems = 0
				if (items.any())
					totalItems += 1 + items.size

				if (files.any())
					totalItems += 1 + files.size

				if (totalItems <= minVisibleItemsForScroll) emptyList()
				else buildList {
					add(Pair(stringResources.top, 0f))

					if (files.any() && items.size > minVisibleItemsForScroll) {
						add(Pair(stringResources.files, (1f + items.size) / totalItems))
					}

					add(Pair(stringResources.end, 1f))
				}
			}
		}

		val progressAnchors by remember {
			derivedStateOf { labeledAnchors.map { (_, p) -> p }.toFloatArray() }
		}

		val anchoredScrollConnectionState = rememberAnchoredScrollConnectionState(progressAnchors)

		BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
			if (maxWidth < Dimensions.twoColumnThreshold) {
				val expandedHeightPx = LocalDensity.current.remember { appBarAndTitleHeight.toPx() }
				val collapsedHeightPx = LocalDensity.current.remember { collapsedHeight.toPx() }
				val rowHeightPx = LocalDensity.current.remember { standardRowHeight.toPx() }

				val fullListSize by LocalDensity.current.remember(maxHeight) {
					val topMenuHeightPx = 0f
					val headerHeightPx = (topMenuHeight + viewPaddingUnit * 2).toPx()
					val rowHeightPx = standardRowHeight.toPx()
					val dividerHeight = 1.dp.toPx()

					derivedStateOf {
						var fullListSize = topMenuHeightPx
						if (items.any()) {
							fullListSize += headerHeightPx + rowHeightPx * items.size + dividerHeight * items.size - 1
						}

						if (files.any()) {
							fullListSize += headerHeightPx + rowHeightPx * files.size + dividerHeight * files.size - 1
						}

						fullListSize -= maxHeight.toPx()
						if (files.any() || items.any())
							fullListSize += rowHeightPx + dividerHeight
						fullListSize.coerceAtLeast(0f)
					}
				}

				val titleHeightScaler = rememberFullScreenScrollConnectedScaler(expandedHeightPx, collapsedHeightPx)
				val menuHeightScaler = rememberPreScrollConnectedScaler(rowHeightPx, 0f)
				val compositeScrollConnection = remember(titleHeightScaler, menuHeightScaler) {
					ConsumedOffsetErasingNestedScrollConnection(
						LinkedNestedScrollConnection(titleHeightScaler, menuHeightScaler)
					)
				}

				val anchoredScrollConnectionDispatcher =
					rememberAutoCloseable(anchoredScrollConnectionState, fullListSize, compositeScrollConnection) {
						AnchoredProgressScrollConnectionDispatcher(
							anchoredScrollConnectionState,
							-fullListSize,
							compositeScrollConnection
						)
					}

				Box(
					modifier = Modifier
						.fillMaxSize()
						.nestedScroll(anchoredScrollConnectionDispatcher)
				) {
					ItemListView(
						itemListViewModel,
						fileListViewModel,
						itemDataLoader,
						nowPlayingViewModel,
						itemListMenuBackPressedHandler,
						trackHeadlineViewModelProvider,
						childItemViewModelProvider,
						applicationNavigation,
						playbackLibraryItems,
						playbackServiceController,
						undoBackStack,
						lazyListState,
						anchoredScrollConnectionState,
						{ _, p -> labeledAnchors.firstOrNull { (_, lp) -> p == lp }?.let { (s, _) -> Text(s) } },
						anchoredScrollConnectionDispatcher::progressTo,
						appBarAndTitleHeight + topMenuHeight + rowPadding * 2
					)

					Column(
						modifier = Modifier
							.background(MaterialTheme.colors.surface)
							.fillMaxWidth()
					) headerColumn@{
						val titleHeightValue by titleHeightScaler.valueState

						val headerCollapseProgress by titleHeightScaler.progressState
						val titleHeightValueDp by LocalDensity.current.run { derivedStateOf { titleHeightValue.toDp() } }
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.background(MaterialTheme.colors.surface)
								.height(titleHeightValueDp)
						) {
							BackButton(
								applicationNavigation::navigateUp,
								modifier = Modifier
									.align(Alignment.TopStart)
									.padding(topRowOuterPadding)
							)

							val menuHeightProgress by menuHeightScaler.progressState
							val chevronRotation by remember {
								derivedStateOf {
									linearInterpolation(
										0f,
										180f,
										menuHeightProgress
									)
								}
							}
							val isMenuFullyShown by remember { derivedStateOf { menuHeightProgress < .02f } }
							val chevronLabel =
								stringResource(id = if (isMenuFullyShown) R.string.collapse else R.string.expand)

							val scope = rememberCoroutineScope()

							ColumnMenuIcon(
								onClick = {
									scope.launch {
										if (!isMenuFullyShown) {
											menuHeightScaler.animateGoToMax()
										} else {
											menuHeightScaler.animateGoToMin()
										}
									}
								},
								icon = {
									Icon(
										painter = painterResource(id = R.drawable.chevron_up_white_36dp),
										tint = LocalControlColor.current,
										contentDescription = chevronLabel,
										modifier = Modifier
											.size(topMenuIconSize)
											.rotate(chevronRotation),
									)
								},
								modifier = Modifier
									.align(Alignment.TopEnd)
									.padding(
										vertical = topRowOuterPadding,
										horizontal = viewPaddingUnit * 2
									),
							)

							ProvideTextStyle(MaterialTheme.typography.h5) {
								val startPadding by rememberTitleStartPadding(titleHeightScaler.progressState)

								val topPadding by remember {
									derivedStateOf {
										linearInterpolation(
											appBarHeight,
											14.dp,
											headerCollapseProgress
										)
									}
								}

								val endPadding by remember {
									derivedStateOf {
										linearInterpolation(
											viewPaddingUnit * 2,
											topMenuIconSize + viewPaddingUnit * 4,
											headerCollapseProgress
										)
									}
								}

								val maxLines by remember { derivedStateOf { (2 - headerCollapseProgress).roundToInt() } }
								if (maxLines > 1) {
									Text(
										text = itemValue,
										maxLines = maxLines,
										overflow = TextOverflow.Ellipsis,
										modifier = Modifier
											.fillMaxWidth()
											.padding(start = startPadding, top = topPadding, end = endPadding),
									)
								} else {
									MarqueeText(
										text = itemValue,
										overflow = TextOverflow.Ellipsis,
										gradientSides = setOf(GradientSide.End),
										gradientEdgeColor = MaterialTheme.colors.surface,
										modifier = Modifier
											.fillMaxWidth()
											.padding(start = startPadding, top = topPadding, end = endPadding),
										isMarqueeEnabled = !lazyListState.isScrollInProgress
									)
								}
							}
						}

						val menuHeightValue by menuHeightScaler.valueState
						val menuHeightDp by LocalDensity.current.remember { derivedStateOf { menuHeightValue.toDp() } }
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.background(MaterialTheme.colors.surface)
								.height(menuHeightDp)
								.clip(RectangleShape)
						) {
							ItemListMenu(
								itemListViewModel = itemListViewModel,
								fileListViewModel = fileListViewModel,
								itemDataLoader = itemDataLoader,
								applicationNavigation = applicationNavigation,
								playbackServiceController = playbackServiceController,
							)
						}
					}
				}
			} else {
				Row(
					modifier = Modifier.fillMaxSize(),
				) {
					val menuWidth =
						this@ItemListView.screenHeight.coerceIn(minimumMenuWidth, this@ItemListView.maxWidth / 2)
					Column(
						modifier = Modifier.width(menuWidth),
					) {
						Column(
							modifier = Modifier.weight(1f)
						) {
							BackButton(
								applicationNavigation::navigateUp,
								modifier = Modifier.padding(topRowOuterPadding)
							)

							ProvideTextStyle(MaterialTheme.typography.h5) {
								Text(
									text = itemValue,
									maxLines = 2,
									overflow = TextOverflow.Ellipsis,
									modifier = Modifier
										.fillMaxWidth()
										.padding(horizontal = viewPaddingUnit),
								)
							}
						}

						ItemListMenu(
							itemListViewModel = itemListViewModel,
							fileListViewModel = fileListViewModel,
							itemDataLoader = itemDataLoader,
							applicationNavigation = applicationNavigation,
							playbackServiceController = playbackServiceController,
							modifier = Modifier.fillMaxWidth(),
						)
					}

					ItemListView(
						itemListViewModel,
						fileListViewModel,
						itemDataLoader,
						nowPlayingViewModel,
						itemListMenuBackPressedHandler,
						trackHeadlineViewModelProvider,
						childItemViewModelProvider,
						applicationNavigation,
						playbackLibraryItems,
						playbackServiceController,
						undoBackStack,
						lazyListState,
						anchoredScrollConnectionState,
						{ _, p -> labeledAnchors.firstOrNull { (_, lp) -> p == lp }?.let { (s, _) -> Text(s) } },
						{ anchoredScrollConnectionState.selectedProgress = it },
					)
				}
			}
		}
	}
}
