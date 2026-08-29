package com.lasthopesoftware.bluewater.client.stored.library.items

import VerticalHeaderScaffold
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.AnchoredChips
import com.lasthopesoftware.bluewater.android.ui.components.AnchoredScrollConnectionState
import com.lasthopesoftware.bluewater.android.ui.components.DeferredPreScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.FullScreenScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.android.ui.components.ListLoading
import com.lasthopesoftware.bluewater.android.ui.components.ListMenuRow
import com.lasthopesoftware.bluewater.android.ui.components.ignoreConsumedOffset
import com.lasthopesoftware.bluewater.android.ui.components.linkedTo
import com.lasthopesoftware.bluewater.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.appBarHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.expandedTitleHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowScrollPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.standardRowHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.LoadItemData
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListContentType
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemsCountHeader
import com.lasthopesoftware.bluewater.client.browsing.items.list.MenuListNavigationFocusRefs
import com.lasthopesoftware.bluewater.client.browsing.items.list.PlaybackLibraryItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.UndoStack
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.toPromise
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

private val boxHeight = expandedTitleHeight + appBarHeight

@Composable
private fun StoredItemsMenu(
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier
	) {
		ListMenuRow(
			modifier = Modifier.fillMaxWidth()
		) {
		}
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChildItem(
	item: IItem,
	itemListViewModel: StoredItemsListViewModel,
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

	val isMenuShown by childItemViewModel.isMenuShown.subscribeAsState()
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

			val isChildItemSynced by childItemViewModel.isSynced.subscribeAsState()
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
private fun ItemListView(
	itemListViewModel: StoredItemsListViewModel,
//	fileListViewModel: FileListViewModel,
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
	modifier: Modifier = Modifier,
	headerHeight: Dp = 0.dp,
	menuListNavigationFocusRefs: MenuListNavigationFocusRefs? = null,
) {
	BoxWithConstraints(modifier = modifier) {
		val isLoading by itemDataLoader.isLoading.subscribeAsState()
		if (isLoading) {
			ListLoading(modifier = Modifier.fillMaxSize())
		} else {
			val items by itemListViewModel.items.subscribeAsState()
//			val files by fileListViewModel.files.subscribeAsState()

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

			var menuFocusBackAction by remember { mutableStateOf<AutoCloseable?>(null) }
			DisposableEffect(Unit) {
				onDispose {
					menuFocusBackAction?.close()
				}
			}
			var modifier = Modifier
				.focusGroup()
				.focusProperties {
					onEnter = {
						menuFocusBackAction?.close()
						menuFocusBackAction = menuListNavigationFocusRefs?.run {
							undoBackStack.addAction {
								onScrollProgress(0f)
								menuFocus.requestFocus().toPromise()
							}
						}
					}
					onExit = {
						if (requestedFocusDirection == FocusDirection.Up)
							onScrollProgress(0f)

						menuFocusBackAction?.close()
						menuFocusBackAction = null
					}
				}
				.scrollbar(
					lazyListState,
					horizontal = false,
					knobColor = MaterialTheme.colors.onSurface,
					trackColor = Color.Transparent,
					visibleAlpha = .4f,
					knobCornerRadius = 1.dp,
				)
			val listFocus = menuListNavigationFocusRefs?.listFocus
			if (listFocus != null)
				modifier = modifier.focusRequester(listFocus)
			LazyColumn(
				state = lazyListState,
				modifier = modifier,
				contentPadding = PaddingValues(top = headerHeight)
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

//				if (files.any()) {
//					item(contentType = ItemListContentType.Header) {
//						FilesCountHeader(files.size)
//					}
//
//					itemsIndexed(files, contentType = { _, _ -> ItemListContentType.File }) { i, f ->
//						RenderTrackTitleItem(
//							i,
//							f,
//							trackHeadlineViewModelProvider,
//							itemListViewModel,
//							nowPlayingViewModel,
//							applicationNavigation,
//							fileListViewModel,
//							itemListMenuBackPressedHandler,
//							playbackServiceController,
//							undoBackStack,
//						)
//
//						if (i < files.lastIndex)
//							Divider()
//					}
//				}
			}

			if (LocalInputModeManager.current.inputMode == InputMode.Touch) {        // 5in in pixels, pixels/Inch
				val maxScrollBarHeight = remember(this@BoxWithConstraints.maxHeight, headerHeight) {
					val dpi = 160f
					(2.5f * dpi).dp.coerceAtMost(this@BoxWithConstraints.maxHeight - headerHeight - topMenuHeight)
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

@Composable
fun BoxWithConstraintsScope.StoredItemsListView(

) {
	ControlSurface {
		if (maxWidth < Dimensions.twoColumnThreshold) {
			val appBarHeightPx = LocalDensity.current.remember { appBarHeight.toPx() }
			val boxHeightPx = LocalDensity.current.remember { boxHeight.toPx() }
			val heightScaler = FullScreenScrollConnectedScaler.remember(min = appBarHeightPx, max = boxHeightPx)
			val topMenuHeightPx =
				LocalDensity.current.remember { topMenuHeight.toPx() + (24.dp + viewPaddingUnit * 2).toPx() }
			val menuHeightScaler = DeferredPreScrollConnectedScaler.remember(topMenuHeightPx, 0f)
			val compositeScroller = remember(heightScaler, menuHeightScaler) {
				heightScaler.linkedTo(menuHeightScaler).ignoreConsumedOffset()
			}

			val listFocus = remember { FocusRequester() }
			VerticalHeaderScaffold(
				header = {},
				overlay = {
					val menuHeightValue by menuHeightScaler.valueState
					val menuHeightValueDp by LocalDensity.current.remember { derivedStateOf { menuHeightValue.toDp() } }
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.height(menuHeightValueDp)
							.clipToBounds()
					) {
						StoredItemsMenu(
							modifier = Modifier
								.graphicsLayer {
									translationY = (menuHeightValue - topMenuHeightPx) * 0.5f
								}
						)
					}
				},
				content = { headerHeight ->

				},
				modifier = Modifier
					.fillMaxSize()
					.nestedScroll(compositeScroller)
			)
		}
	}
}
