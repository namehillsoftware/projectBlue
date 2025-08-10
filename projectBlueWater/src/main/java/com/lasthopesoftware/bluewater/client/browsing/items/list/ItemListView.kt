package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.rememberCalculatedKnobHeight
import com.lasthopesoftware.bluewater.android.ui.components.rememberTitleStartPadding
import com.lasthopesoftware.bluewater.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.DetermineWindowControlColors
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.appBarHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.expandedTitleHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.menuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowScrollPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconSize
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topRowOuterPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
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
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledActiveDownloadsButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledSearchButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledSettingsButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.UnlabelledRefreshButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.UndoStack
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.toPromise
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlin.math.roundToInt

private val boxHeight = expandedTitleHeight + appBarHeight

@Composable
fun ItemsCountHeader(itemsCount: Int) {
	Box(
		modifier = Modifier
			.padding(viewPaddingUnit)
			.height(menuHeight)
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
			.height(menuHeight)
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
	val rowHeight = Dimensions.standardRowHeight
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
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onLongClick = {
					hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

					itemListMenuBackPressedHandler.hideAllMenus()

					childItemViewModel.showMenu()

					backStack.addAction {
						childItemViewModel.hideMenu().toPromise()
					}
				},
				onClickLabel = stringResource(id = R.string.btn_view_song_details),
				onClick = {
					itemListViewModel.loadedLibraryId?.also {
						applicationNavigation.viewItem(it, item)
					}
				},
				scrollPadding = rowHeight.rowScrollPadding
			)
			.height(rowHeight)
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
				.height(rowHeight)
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

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ItemListView(
    itemListViewModel: ItemListViewModel,
    fileListViewModel: FileListViewModel,
    nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
    itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
    trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
    childItemViewModelProvider: PooledCloseablesViewModel<ReusableChildItemViewModel>,
    applicationNavigation: NavigateApplication,
    playbackLibraryItems: PlaybackLibraryItems,
    playbackServiceController: ControlPlaybackService,
    undoBackStack: UndoStack,
) {
	val files by fileListViewModel.files.subscribeAsState()
	val rowHeight = Dimensions.standardRowHeight
	val itemValue by itemListViewModel.itemValue.subscribeAsState()

	val lazyListState = rememberLazyListState()

	val refreshButtonFocus = remember { FocusRequester() }

	@Composable
	fun BoxWithConstraintsScope.LoadedItemListView() {
		val items by itemListViewModel.items.subscribeAsState()

		val knobHeight by rememberCalculatedKnobHeight(lazyListState, rowHeight)
		LazyColumn(
			state = lazyListState,
			modifier = Modifier
				.focusGroup()
				.scrollbar(
					lazyListState,
					horizontal = false,
					knobColor = MaterialTheme.colors.onSurface,
					trackColor = Color.Transparent,
					visibleAlpha = .4f,
					knobCornerRadius = 1.dp,
					fixedKnobRatio = knobHeight,
				),
		) {
			item(contentType = ItemListContentType.Menu) {
				Row(
					modifier = Modifier
						.padding(rowPadding)
						.fillMaxWidth()
						.focusGroup(),
					horizontalArrangement = Arrangement.SpaceEvenly,
				) {
					if (files.any()) {
						LabelledPlayButton(
							libraryState = itemListViewModel,
							playbackServiceController = playbackServiceController,
							serviceFilesListState = fileListViewModel,
						)

						LabelledSyncButton(fileListViewModel)

						LabelledShuffleButton(
							libraryState = itemListViewModel,
							playbackServiceController = playbackServiceController,
							serviceFilesListState = fileListViewModel,
						)
					} else {
						LabelledActiveDownloadsButton(
							itemListViewModel = itemListViewModel,
							applicationNavigation = applicationNavigation,
						)

						LabelledSettingsButton(
							itemListViewModel,
							applicationNavigation
						)

						LabelledSearchButton(
							itemListViewModel = itemListViewModel,
							applicationNavigation = applicationNavigation,
						)
					}
				}
			}

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

			if (!files.any()) return@LazyColumn

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

	val isFilesLoading by fileListViewModel.isLoading.subscribeAsState()

	BoxWithConstraints(modifier = Modifier.fillMaxSize().focusGroup()) {
		ControlSurface {
			DetermineWindowControlColors()
			val isItemsLoading by itemListViewModel.isLoading.subscribeAsState()

			val collapsedHeight = appBarHeight

			val expandedHeightPx = LocalDensity.current.run { boxHeight.toPx() }
			val collapsedHeightPx = LocalDensity.current.run { collapsedHeight.toPx() }
			val heightScaler = memorableScrollConnectedScaler(expandedHeightPx, collapsedHeightPx)

			val scope = rememberCoroutineScope()

			val isAtTop by remember {
				derivedStateOf {
					lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
				}
			}

			val inputMode = LocalInputModeManager.current
			DisposableEffect(isAtTop, inputMode, heightScaler, lazyListState) {
				if (isAtTop) {
					onDispose { }
				} else {
					val scrollToTopAction = {
						scope.async {
							if (lazyListState.firstVisibleItemIndex <= 0) false
							else {
								heightScaler.goToMax()
								lazyListState.scrollToItem(0)
								if (inputMode.inputMode == InputMode.Keyboard)
									refreshButtonFocus.requestFocus()
								true
							}
						}.toPromise()
					}

					undoBackStack.addAction(scrollToTopAction)

					onDispose {
						undoBackStack.removeAction(scrollToTopAction)
					}
				}
			}

			val isHeaderTall by remember { derivedStateOf { (boxHeight + menuHeight) * 2 < maxHeight } }
			Column(
				modifier = Modifier
					.fillMaxSize()
					.nestedScroll(heightScaler)
			) {
				if (isHeaderTall) {
					Column(modifier = Modifier.background(MaterialTheme.colors.surface)) headerColumn@{
						val heightValue by heightScaler.getValueState()

						Box(
							modifier = Modifier
								.fillMaxWidth()
								.height(LocalDensity.current.run { heightValue.toDp() })
						) {
							BackButton(
								applicationNavigation::navigateUp,
								modifier = Modifier
									.align(Alignment.TopStart)
									.padding(topRowOuterPadding)
							)

							UnlabelledRefreshButton(
								itemListViewModel,
								fileListViewModel,
								Modifier
									.align(Alignment.TopEnd)
									.padding(
										vertical = topRowOuterPadding,
										horizontal = viewPaddingUnit * 2
									),
								refreshButtonFocus
							)

							ProvideTextStyle(MaterialTheme.typography.h5) {
								val startPadding by rememberTitleStartPadding(heightScaler.getProgressState())

								val headerCollapseProgress by heightScaler.getProgressState()

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
					}
				} else {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.height(collapsedHeight),
						verticalAlignment = Alignment.CenterVertically,
					) {
						BackButton(
							applicationNavigation::navigateUp,
							modifier = Modifier.padding(horizontal = topRowOuterPadding)
						)

						ProvideTextStyle(MaterialTheme.typography.h5) {
							MarqueeText(
								text = itemValue,
								overflow = TextOverflow.Ellipsis,
								gradientSides = setOf(GradientSide.End),
								gradientEdgeColor = MaterialTheme.colors.surface,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = viewPaddingUnit)
									.weight(1f),
								isMarqueeEnabled = !lazyListState.isScrollInProgress
							)
						}

						if (!isFilesLoading && !isItemsLoading) {
							UnlabelledRefreshButton(
								itemListViewModel,
								fileListViewModel,
								Modifier.padding(horizontal = viewPaddingUnit)
							)
						}
					}
				}

				BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
					val isLoaded = !isItemsLoading && !isFilesLoading

					if (isLoaded) LoadedItemListView()
					else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
				}
			}
		}
	}
}
