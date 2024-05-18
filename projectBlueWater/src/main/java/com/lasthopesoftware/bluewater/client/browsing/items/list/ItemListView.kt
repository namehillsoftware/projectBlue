package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeaderItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.memorableScrollConnectedScaler
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberCalculatedKnobHeight
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class ContentType {
	Header, Spacer, Item, File
}

private const val expandedTitleHeight = 84
private val appBarHeight = Dimensions.appBarHeight.value
private val iconSize = Dimensions.topMenuIconSize
private val minimumMenuWidth = (iconSize + Dimensions.viewPaddingUnit * 2) * 3

private val expandedIconSize = Dimensions.menuHeight.value
private const val expandedMenuVerticalPadding = 12
private val boxHeight = (expandedTitleHeight + appBarHeight + expandedIconSize + expandedMenuVerticalPadding * 2).dp

@Composable
private fun RowScope.LabelledPlayButton(
	itemListViewModel: ItemListViewModel,
	playbackServiceController: ControlPlaybackService,
	fileListViewModel: FileListViewModel,
	modifier: Modifier,
) {
	val playButtonLabel = stringResource(id = R.string.btn_play)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				playbackServiceController.startPlaylist(it, fileListViewModel.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_play),
		contentDescription = playButtonLabel,
		label = playButtonLabel,
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
private fun RowScope.UnlabelledPlayButton(
	itemListViewModel: ItemListViewModel,
	playbackServiceController: ControlPlaybackService,
	fileListViewModel: FileListViewModel,
) {
	val playButtonLabel = stringResource(id = R.string.btn_play)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				playbackServiceController.startPlaylist(it, fileListViewModel.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_play),
		contentDescription = playButtonLabel,
		label = null,
	)
}

@Composable
private fun RowScope.LabelledSyncButton(
	fileListViewModel: FileListViewModel,
	modifier: Modifier,
) {
	val isSynced by fileListViewModel.isSynced.collectAsState()
	val syncButtonLabel =
		if (!isSynced) stringResource(id = R.string.btn_sync_item)
		else stringResource(id = R.string.files_synced)
	ColumnMenuIcon(
		onClick = { fileListViewModel.toggleSync() },
		icon = {
			SyncIcon(
				isActive = isSynced,
				modifier = Modifier.size(iconSize),
				contentDescription = syncButtonLabel,
			)
		},
		label = syncButtonLabel,
		labelMaxLines = 1,
		labelModifier = modifier,
	)
}

@Composable
private fun RowScope.UnlabelledSyncButton(fileListViewModel: FileListViewModel) {
	val isSynced by fileListViewModel.isSynced.collectAsState()
	val syncButtonLabel =
		if (!isSynced) stringResource(id = R.string.btn_sync_item)
		else stringResource(id = R.string.files_synced)
	ColumnMenuIcon(
		onClick = { fileListViewModel.toggleSync() },
		icon = {
			SyncIcon(
				isActive = isSynced,
				modifier = Modifier.size(iconSize),
				contentDescription = syncButtonLabel,
			)
		},
	)
}

@Composable
private fun RowScope.LabelledShuffleButton(
	itemListViewModel: ItemListViewModel,
	playbackServiceController: ControlPlaybackService,
	fileListViewModel: FileListViewModel,
	modifier: Modifier,
) {
	val shuffleButtonLabel = stringResource(R.string.btn_shuffle_files)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				playbackServiceController.shuffleAndStartPlaylist(it, fileListViewModel.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_shuffle),
		contentDescription = shuffleButtonLabel,
		label = shuffleButtonLabel,
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
private fun RowScope.UnlabelledShuffleButton(
	itemListViewModel: ItemListViewModel,
	playbackServiceController: ControlPlaybackService,
	fileListViewModel: FileListViewModel,
) {
	val shuffleButtonLabel = stringResource(R.string.btn_shuffle_files)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				playbackServiceController.shuffleAndStartPlaylist(it, fileListViewModel.files.value)
			}
		},
		iconPainter = painterResource(id = R.drawable.av_shuffle),
		contentDescription = shuffleButtonLabel,
	)
}

@Composable
fun RowScope.LabelledActiveDownloadsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
	modifier: Modifier,
) {
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				applicationNavigation.viewActiveDownloads(it)
			}
		},
		iconPainter = painterResource(id = R.drawable.ic_water),
		contentDescription = stringResource(id = R.string.activeDownloads),
		label = stringResource(id = R.string.downloads), // Use shortened version for button size
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
fun RowScope.UnlabelledActiveDownloadsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
) {
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also {
				applicationNavigation.viewActiveDownloads(it)
			}
		},
		iconPainter = painterResource(id = R.drawable.ic_water),
		contentDescription = stringResource(id = R.string.activeDownloads),
	)
}

@Composable
fun RowScope.LabelledSearchButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
	modifier: Modifier,
) {
	val searchButtonLabel = stringResource(id = R.string.search)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::launchSearch)
		},
		iconPainter = painterResource(id = R.drawable.search_36dp),
		contentDescription = searchButtonLabel,
		label = searchButtonLabel,
		labelMaxLines = 1,
		labelModifier = modifier,
	)
}

@Composable
fun RowScope.UnlabelledSearchButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
) {
	val searchButtonLabel = stringResource(id = R.string.search)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::launchSearch)
		},
		iconPainter = painterResource(id = R.drawable.search_36dp),
		contentDescription = searchButtonLabel,
	)
}

@Composable
fun RowScope.LabelledSettingsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
	modifier: Modifier,
) {
	val settingsButtonLabel = stringResource(id = R.string.settings)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::viewServerSettings)
		},
		iconPainter = painterResource(id = R.drawable.ic_action_settings),
		contentDescription = settingsButtonLabel,
		label = settingsButtonLabel,
		labelModifier = modifier,
		labelMaxLines = 1,
	)
}

@Composable
fun RowScope.UnlabelledSettingsButton(
	itemListViewModel: ItemListViewModel,
	applicationNavigation: NavigateApplication,
) {
	val settingsButtonLabel = stringResource(id = R.string.settings)
	ColumnMenuIcon(
		onClick = {
			itemListViewModel.loadedLibraryId?.also(applicationNavigation::viewServerSettings)
		},
		iconPainter = painterResource(id = R.drawable.ic_action_settings),
		contentDescription = settingsButtonLabel,
	)
}

@Composable
private fun BoxScope.CollapsedItemListMenu(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	menuPaddingValues: PaddingValues,
	menuWidth: Dp
) {
	Row(
		modifier = Modifier
			.padding(
				top = menuPaddingValues.calculateTopPadding(),
				bottom = menuPaddingValues.calculateBottomPadding(),
				start = menuPaddingValues.calculateStartPadding(LocalLayoutDirection.current),
				end = menuPaddingValues.calculateEndPadding(LocalLayoutDirection.current),
			)
			.width(menuWidth)
			.align(Alignment.TopEnd)
	) {
		val files by fileListViewModel.files.collectAsState()
		if (files.any()) {
			UnlabelledPlayButton(
				itemListViewModel = itemListViewModel,
				playbackServiceController = playbackServiceController,
				fileListViewModel = fileListViewModel
			)

			UnlabelledSyncButton(fileListViewModel)

			UnlabelledShuffleButton(
				itemListViewModel = itemListViewModel,
				playbackServiceController = playbackServiceController,
				fileListViewModel = fileListViewModel
			)
		} else {
			UnlabelledActiveDownloadsButton(
				itemListViewModel = itemListViewModel,
				applicationNavigation = applicationNavigation
			)

			UnlabelledSearchButton(
				itemListViewModel = itemListViewModel,
				applicationNavigation = applicationNavigation
			)

			UnlabelledSettingsButton(
				itemListViewModel = itemListViewModel,
				applicationNavigation = applicationNavigation
			)
		}
	}
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
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
) {
	val playingFile by nowPlayingViewModel.nowPlayingFile.subscribeAsState()
	val files by fileListViewModel.files.collectAsState()
	val rowHeight = Dimensions.standardRowHeight
	val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }
	val hapticFeedback = LocalHapticFeedback.current
	val itemValue by itemListViewModel.itemValue.collectAsState()

	@Composable
	fun ChildItem(item: IItem, onNavigatedTo: () -> Unit) {
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

		if (!isMenuShown) {
			Box(modifier = Modifier
				.navigable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onLongClick = {
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

						itemListMenuBackPressedHandler.hideAllMenus()

						childItemViewModel.showMenu()
					},
					onClickLabel = stringResource(id = R.string.btn_view_song_details),
					onClick = {
						itemListViewModel.loadedLibraryId?.also {
							applicationNavigation.viewItem(it, item)
						}
					},
					onNavigatedTo = onNavigatedTo,
				)
				.height(rowHeight)
				.fillMaxSize()
			) {
				Text(
					text = item.value ?: "",
					fontSize = rowFontSize,
					overflow = TextOverflow.Ellipsis,
					maxLines = 1,
					fontWeight = FontWeight.Normal,
					modifier = Modifier
						.padding(Dimensions.viewPaddingUnit * 3)
						.align(Alignment.CenterStart),
				)
			}

			return
		}

		Row(
			modifier = Modifier
				.height(rowHeight)
				.padding(8.dp)
		) {
			ListItemIcon(
				painter = painterResource(id = R.drawable.av_play),
				contentDescription = stringResource(id = R.string.btn_play),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable {
						itemListViewModel.loadedLibraryId?.also {
							playbackLibraryItems.playItem(it, ItemId(item.key))
						}
					}
					.align(Alignment.CenterVertically),
			)

			val isChildItemSynced by childItemViewModel.isSynced.collectAsState()
			SyncIcon(
				isActive = isChildItemSynced,
				modifier = Modifier
					.fillMaxWidth()
					.clickable { childItemViewModel.toggleSync() }
					.weight(1f)
					.align(Alignment.CenterVertically),
			)

			ListItemIcon(
				painter = painterResource(id = R.drawable.av_shuffle),
				contentDescription = stringResource(id = R.string.btn_shuffle_files),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable {
						itemListViewModel.loadedLibraryId?.also {
							playbackLibraryItems.playItemShuffled(it, ItemId(item.key))
						}
					}
					.align(Alignment.CenterVertically),
			)
		}
	}

	@Composable
	fun RenderTrackHeaderItem(position: Int, serviceFile: ServiceFile) {
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
		val isPlaying by remember(serviceFile) { derivedStateOf { playingFile?.serviceFile == serviceFile } }

		val viewFileDetailsClickHandler = remember(position) {
			{
				itemListViewModel.loadedLibraryId?.also {
					applicationNavigation.viewFileDetails(it, files, position)
				}
				Unit
			}
		}

		TrackHeaderItemView(
			itemName = fileName,
			isActive = isPlaying,
			isHiddenMenuShown = isMenuShown,
			onItemClick = viewFileDetailsClickHandler,
			onHiddenMenuClick = {
				itemListMenuBackPressedHandler.hideAllMenus()
				fileItemViewModel.showMenu()
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
					playbackServiceController.startPlaylist(it, files, position)
				}
			}
		)
	}

	val lazyListState = rememberLazyListState()
	val scope = rememberCoroutineScope()

	@Composable
	fun BoxWithConstraintsScope.LoadedItemListView(headerHeight: Dp) {
		val items by itemListViewModel.items.collectAsState()

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
			item(contentType = ContentType.Spacer) {
				Spacer(modifier = Modifier
					.requiredHeight(headerHeight)
					.fillMaxWidth()
					.focusable(false))
			}

			if (items.any()) {
				item(contentType = ContentType.Header) {
					Box(
						modifier = Modifier
							.padding(Dimensions.viewPaddingUnit)
							.height(Dimensions.menuHeight)
					) {
						ProvideTextStyle(MaterialTheme.typography.h5) {
							Text(
								text = stringResource(R.string.item_count_label, items.size),
								fontWeight = FontWeight.Bold,
								modifier = Modifier
									.padding(Dimensions.viewPaddingUnit)
									.align(Alignment.CenterStart)
							)
						}
					}
				}

				val itemHeaderCount = 2

				itemsIndexed(items, { _, i -> i.key }, { _, _ -> ContentType.Item }) { i, f ->
					ChildItem(f) {
						if (lazyListState.canScrollForward) {
							val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
							if (lastVisibleItem != null && lastVisibleItem.index == i + itemHeaderCount) {
								scope.launch {
									lazyListState.scrollBy(lastVisibleItem.size * 2f)
								}
							}
						}

						if (lazyListState.canScrollBackward) {
							val firstVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
							if (firstVisibleItem != null && firstVisibleItem.index == i - itemHeaderCount) {
								scope.launch {
									lazyListState.scrollBy(firstVisibleItem.size * -2f)
								}
							}
						}
					}

					if (i < items.lastIndex)
						Divider()
				}
			}

			if (!files.any()) return@LazyColumn

			item(contentType = ContentType.Header) {
				Box(
					modifier = Modifier
						.padding(Dimensions.viewPaddingUnit)
						.height(Dimensions.menuHeight)
				) {
					ProvideTextStyle(MaterialTheme.typography.h5) {
						Text(
							text = stringResource(R.string.file_count_label, files.size),
							fontWeight = FontWeight.Bold,
							modifier = Modifier
								.padding(Dimensions.viewPaddingUnit)
								.align(Alignment.CenterStart)
						)
					}
				}
			}

			itemsIndexed(files, contentType = { _, _ -> ContentType.File }) { i, f ->
				RenderTrackHeaderItem(i, f)

				if (i < files.lastIndex)
					Divider()
			}
		}
	}

	val isFilesLoading by fileListViewModel.isLoading.subscribeAsState()

	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		ControlSurface {

			val collapsedHeight = appBarHeight.dp

			val expandedHeightPx = LocalDensity.current.run { boxHeight.toPx() }
			val collapsedHeightPx = LocalDensity.current.run { collapsedHeight.toPx() }
			val heightScaler = memorableScrollConnectedScaler(expandedHeightPx, collapsedHeightPx)

			val isHeaderTall by remember { derivedStateOf { boxHeight * 2 < maxHeight } }
			val actualExpandedHeight by remember { derivedStateOf { if (isHeaderTall) boxHeight else collapsedHeight } }
			Box(
				modifier = Modifier
					.fillMaxSize()
					.nestedScroll(heightScaler)
			) {
				BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
					val isItemsLoading by itemListViewModel.isLoading.subscribeAsState()
					val isLoaded = !isItemsLoading && !isFilesLoading

					if (isLoaded) LoadedItemListView(actualExpandedHeight)
					else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
				}

				val collapsedTopRowPadding = 2.dp
				if (isHeaderTall) {
					val heightValue by heightScaler.getValueState()
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.height(LocalDensity.current.run { heightValue.toDp() })
					) {
						Icon(
							Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "",
							tint = MaterialTheme.colors.onSurface,
							modifier = Modifier
								.align(Alignment.TopStart)
								.clickable(
									onClick = applicationNavigation::navigateUp,
									indication = null,
									interactionSource = remember { MutableInteractionSource() }
								)
								.padding(Dimensions.viewPaddingUnit * 4)
						)

						val headerCollapseProgress by heightScaler.getProgressState()
						val topPadding by remember { derivedStateOf { (appBarHeight - 42 * headerCollapseProgress).dp } }
						BoxWithConstraints(modifier = Modifier.padding(top = topPadding)) nestedBoxScope@{
							val acceleratedToolbarStateProgress by remember {
								derivedStateOf {
									(1 - headerCollapseProgress).pow(3).coerceIn(0f, 1f)
								}
							}

							val acceleratedHeaderHidingProgress by remember {
								derivedStateOf { 1 - acceleratedToolbarStateProgress }
							}

							ProvideTextStyle(MaterialTheme.typography.h5) {
								val startPadding by remember { derivedStateOf { (4 + 48 * headerCollapseProgress).dp } }
								val endPadding by remember { derivedStateOf { Dimensions.viewPaddingUnit + minimumMenuWidth * acceleratedHeaderHidingProgress } }
								val maxLines by remember { derivedStateOf { (2 - headerCollapseProgress).roundToInt() } }
								if (maxLines > 1) {
									Text(
										text = itemValue,
										maxLines = maxLines,
										overflow = TextOverflow.Ellipsis,
										modifier = Modifier
											.fillMaxWidth()
											.padding(start = startPadding, end = endPadding),
									)
								} else {
									MarqueeText(
										text = itemValue,
										overflow = TextOverflow.Ellipsis,
										gradientSides = setOf(GradientSide.End),
										gradientEdgeColor = MaterialTheme.colors.surface,
										modifier = Modifier
											.fillMaxWidth()
											.padding(start = startPadding, end = endPadding),
										isMarqueeEnabled = !lazyListState.isScrollInProgress
									)
								}
							}

							if (isFilesLoading) return@nestedBoxScope

							val menuWidth by remember {
								derivedStateOf {
									linearInterpolation(
										maxWidth,
										minimumMenuWidth,
										acceleratedHeaderHidingProgress
									)
								}
							}

							val expandedTopRowPadding = expandedTitleHeight.dp + expandedMenuVerticalPadding.dp
							val topRowPadding by remember {
								derivedStateOf {
									linearInterpolation(
										expandedTopRowPadding,
										collapsedTopRowPadding,
										headerCollapseProgress
									)
								}
							}

							if (acceleratedHeaderHidingProgress < 1) {
								val textModifier = Modifier.alpha(acceleratedToolbarStateProgress)
								Row(
									modifier = Modifier
										.padding(
											top = topRowPadding,
											bottom = expandedMenuVerticalPadding.dp,
											start = Dimensions.viewPaddingUnit * 2,
											end = Dimensions.viewPaddingUnit * 2
										)
										.width(menuWidth)
										.align(Alignment.TopEnd),
									horizontalArrangement = Arrangement.SpaceEvenly,
								) {
									if (files.any()) {
										LabelledPlayButton(
											itemListViewModel = itemListViewModel,
											playbackServiceController = playbackServiceController,
											fileListViewModel = fileListViewModel,
											modifier = textModifier,
										)

										LabelledSyncButton(
											fileListViewModel = fileListViewModel,
											modifier = textModifier
										)

										LabelledShuffleButton(
											itemListViewModel = itemListViewModel,
											playbackServiceController = playbackServiceController,
											fileListViewModel = fileListViewModel,
											modifier = textModifier
										)
									} else {
										LabelledActiveDownloadsButton(
											itemListViewModel = itemListViewModel,
											applicationNavigation = applicationNavigation,
											modifier = textModifier
										)

										LabelledSearchButton(
											itemListViewModel = itemListViewModel,
											applicationNavigation = applicationNavigation,
											modifier = textModifier
										)

										LabelledSettingsButton(
											itemListViewModel = itemListViewModel,
											applicationNavigation = applicationNavigation,
											modifier = textModifier
										)
									}
								}
							} else {
								CollapsedItemListMenu(
									itemListViewModel = itemListViewModel,
									fileListViewModel = fileListViewModel,
									applicationNavigation = applicationNavigation,
									playbackServiceController = playbackServiceController,
									menuPaddingValues = PaddingValues(
										top = topRowPadding,
										bottom = expandedMenuVerticalPadding.dp,
										start = Dimensions.viewPaddingUnit * 2,
										end = Dimensions.viewPaddingUnit * 2
									),
									menuWidth = menuWidth,
								)
							}
						}
					}
				} else {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.height(collapsedHeight)
					) {
						Icon(
							Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "",
							tint = MaterialTheme.colors.onSurface,
							modifier = Modifier
								.align(Alignment.TopStart)
								.clickable(
									interactionSource = remember { MutableInteractionSource() },
									indication = null,
									onClick = applicationNavigation::navigateUp
								)
								.padding(Dimensions.viewPaddingUnit * 4)
						)

						val topPadding = (appBarHeight - 42).dp
						BoxWithConstraints(modifier = Modifier.padding(top = topPadding)) nestedBoxScope@{
							ProvideTextStyle(MaterialTheme.typography.h5) {
								MarqueeText(
									text = itemValue,
									overflow = TextOverflow.Ellipsis,
									gradientSides = setOf(GradientSide.End),
									gradientEdgeColor = MaterialTheme.colors.surface,
									modifier = Modifier
										.fillMaxWidth()
										.padding(
											start = Dimensions.viewPaddingUnit * 13,
											end = Dimensions.viewPaddingUnit + minimumMenuWidth
										),
									isMarqueeEnabled = !lazyListState.isScrollInProgress
								)
							}

							if (isFilesLoading) return@nestedBoxScope

							val menuWidth = minimumMenuWidth

							CollapsedItemListMenu(
								itemListViewModel = itemListViewModel,
								fileListViewModel = fileListViewModel,
								applicationNavigation = applicationNavigation,
								playbackServiceController = playbackServiceController,
								menuPaddingValues = PaddingValues(
									top = collapsedTopRowPadding,
									bottom = expandedMenuVerticalPadding.dp,
									start = Dimensions.viewPaddingUnit * 2,
									end = Dimensions.viewPaddingUnit * 2
								),
								menuWidth = menuWidth,
							)
						}
					}
				}
			}
		}
	}
}
