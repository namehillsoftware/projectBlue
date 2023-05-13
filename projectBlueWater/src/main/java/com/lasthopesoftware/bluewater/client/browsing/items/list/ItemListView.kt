package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeaderItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.*
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.ColumnMenuIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.ListItemIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.rememberCalculatedKnobHeight
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.ui.theme.*
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import kotlin.math.pow
import kotlin.math.roundToInt

private const val expandedTitleHeight = 84
private val appBarHeight = Dimensions.appBarHeight.value
private val iconSize = Dimensions.topMenuIconSize
private val minimumMenuWidth = iconSize * 3

@OptIn(ExperimentalFoundationApi::class)
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
	val playingFile by nowPlayingViewModel.nowPlayingFile.collectAsState()
	val files by fileListViewModel.files.collectAsState()
	val rowHeight = Dimensions.standardRowHeight
	val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }
	val hapticFeedback = LocalHapticFeedback.current
	val itemValue by itemListViewModel.itemValue.collectAsState()

	@Composable
	fun ChildItem(item: IItem) {
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
				.combinedClickable(
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
					}
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
						.padding(12.dp)
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
				 playbackServiceController.addToPlaylist(serviceFile)
			},
			onViewFilesClick = viewFileDetailsClickHandler,
			onPlayClick = {
				fileItemViewModel.hideMenu()
				playbackServiceController.startPlaylist(files, position)
			}
		)
	}

	val lazyListState = rememberLazyListState()

	@Composable
	fun BoxWithConstraintsScope.LoadedItemListView() {
		val items by itemListViewModel.items.collectAsState()

		val knobHeight by rememberCalculatedKnobHeight(lazyListState, rowHeight)
		LazyColumn(
			state = lazyListState,
			modifier = Modifier
				.scrollbar(
					lazyListState,
					horizontal = false,
					knobColor = MaterialTheme.colors.onSurface,
					trackColor = Color.Transparent,
					visibleAlpha = .4f,
					knobCornerRadius = 1.dp,
					fixedKnobRatio = knobHeight,
				)
		) {
			if (items.any()) {
				item {
					Box(
						modifier = Modifier
							.padding(4.dp)
							.height(48.dp)
					) {
						ProvideTextStyle(MaterialTheme.typography.h5) {
							Text(
								text = stringResource(R.string.item_count_label, items.size),
								fontWeight = FontWeight.Bold,
								modifier = Modifier
									.padding(4.dp)
									.align(Alignment.CenterStart)
							)
						}
					}
				}

				itemsIndexed(items, { _, i -> i.key }) { i, f ->
					ChildItem(f)

					if (i < items.lastIndex)
						Divider()
				}
			}

			if (!files.any()) return@LazyColumn

			item {
				Box(
					modifier = Modifier
						.padding(4.dp)
						.height(48.dp)
				) {
					ProvideTextStyle(MaterialTheme.typography.h5) {
						Text(
							text = stringResource(R.string.file_count_label, files.size),
							fontWeight = FontWeight.Bold,
							modifier = Modifier
								.padding(4.dp)
								.align(Alignment.CenterStart)
						)
					}
				}
			}

			itemsIndexed(files) { i, f ->
				RenderTrackHeaderItem(i, f)

				if (i < files.lastIndex)
					Divider()
			}
		}
	}

	val isFilesLoaded by fileListViewModel.isLoaded.collectAsState()

	ControlSurface {
		// Treat the files not being loaded as isAnyFiles being false to trick the CollapsingToolbarScaffold
		// into measuring the expanded size correctly.
		val isAnyFiles by remember { derivedStateOf { !isFilesLoaded || files.any() } }
		val expandedIconSize by remember { derivedStateOf { if (isAnyFiles) Dimensions.menuHeight.value else 0f } }
		val expandedMenuVerticalPadding by remember { derivedStateOf { if (isAnyFiles) 12 else 0 } }
		val boxHeight by remember {
			derivedStateOf {
				expandedTitleHeight + appBarHeight + expandedIconSize + expandedMenuVerticalPadding * 2
			}
		}

		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by remember { derivedStateOf { 1 - toolbarState.toolbarState.progress } }
		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val topPadding by remember { derivedStateOf { (appBarHeight - 46 * headerHidingProgress).dp } }
				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight.dp)
						.padding(top = topPadding)
				) {
					val acceleratedToolbarStateProgress by remember {
						derivedStateOf {
							toolbarState.toolbarState.progress.pow(
								3
							).coerceIn(0f, 1f)
						}
					}
					val acceleratedHeaderHidingProgress by remember {
						derivedStateOf { 1 - acceleratedToolbarStateProgress }
					}
					ProvideTextStyle(MaterialTheme.typography.h5) {
						val startPadding by remember { derivedStateOf { (4 + 48 * headerHidingProgress).dp } }
						val endPadding by remember { derivedStateOf { Dimensions.viewPaddingUnit + minimumMenuWidth * acceleratedHeaderHidingProgress } }
						val maxLines by remember { derivedStateOf { (2 - headerHidingProgress).roundToInt() } }
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

					if (files.any()) {
						val menuWidth by remember { derivedStateOf { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedHeaderHidingProgress) } }
						val expandedTopRowPadding = expandedTitleHeight + expandedMenuVerticalPadding
						val collapsedTopRowPadding = 6
						val topRowPadding by remember { derivedStateOf { (expandedTopRowPadding - (expandedTopRowPadding - collapsedTopRowPadding) * headerHidingProgress).dp } }

						Row(
							modifier = Modifier
								.padding(
									top = topRowPadding,
									bottom = expandedMenuVerticalPadding.dp,
									start = 8.dp,
									end = 8.dp
								)
								.width(menuWidth)
								.align(Alignment.TopEnd)
						) {
							val textModifier = Modifier.alpha(acceleratedToolbarStateProgress)

							val playButtonLabel = stringResource(id = R.string.btn_play)
							ColumnMenuIcon(
								onClick = { playbackServiceController.startPlaylist(files) },
								iconPainter = painterResource(id = R.drawable.av_play),
								contentDescription = playButtonLabel,
								label = if (acceleratedHeaderHidingProgress < 1) playButtonLabel else null,
								labelModifier = textModifier,
								labelMaxLines = 1,
							)

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
								label = if (acceleratedHeaderHidingProgress < 1) syncButtonLabel else null,
								labelMaxLines = 1,
								labelModifier = textModifier,
							)

							val shuffleButtonLabel = stringResource(R.string.btn_shuffle_files)
							ColumnMenuIcon(
								onClick = { playbackServiceController.shuffleAndStartPlaylist(files) },
								iconPainter = painterResource(id = R.drawable.av_shuffle),
								contentDescription = shuffleButtonLabel,
								label = if (acceleratedHeaderHidingProgress < 1) shuffleButtonLabel else null,
								labelModifier = textModifier,
								labelMaxLines = 1,
							)
						}
					}
				}

				Box(modifier = Modifier.height(appBarHeight.dp)) {
					Icon(
						Icons.Default.ArrowBack,
						contentDescription = "",
						tint = MaterialTheme.colors.onSurface,
						modifier = Modifier
							.padding(16.dp)
							.align(Alignment.CenterStart)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								onClick = applicationNavigation::navigateUp
							)
					)
				}
			},
		) {
			BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
				val isItemsLoading by itemListViewModel.isLoading.collectAsState()
				val isLoaded = !isItemsLoading && isFilesLoaded

				if (isLoaded) LoadedItemListView()
				else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}
		}
	}
}
