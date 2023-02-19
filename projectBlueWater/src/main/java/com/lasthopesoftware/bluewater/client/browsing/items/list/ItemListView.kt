package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackHeaderItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.*
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncButton
import com.lasthopesoftware.bluewater.shared.android.ui.components.GradientSide
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.scrollbar
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemListView(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	nowPlayingViewModel: NowPlayingFilePropertiesViewModel,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	trackHeadlineViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
) {
	val playingFile by nowPlayingViewModel.nowPlayingFile.collectAsState()
	val files by fileListViewModel.files.collectAsState()
	val rowHeight = dimensionResource(id = R.dimen.standard_row_height)
	val rowFontSize = LocalDensity.current.run { dimensionResource(id = R.dimen.row_font_size).toSp() }
	val hapticFeedback = LocalHapticFeedback.current
	val itemValue by itemListViewModel.itemValue.collectAsState()

	@Composable
	fun ChildItem(childItemViewModel: ItemListViewModel.ChildItemViewModel) {
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
					onClick = childItemViewModel::viewItem
				)
				.height(rowHeight)
				.fillMaxSize()
			) {
				Text(
					text = childItemViewModel.item.value ?: "",
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

		DisposableEffect(childItemViewModel.item) {
			onDispose {
				childItemViewModel.hideMenu()
			}
		}

		Row(
			modifier = Modifier
				.height(rowHeight)
				.padding(8.dp)
		) {
			Image(
				painter = painterResource(id = R.drawable.av_play),
				contentDescription = stringResource(id = R.string.btn_play),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable(onClick = childItemViewModel::play)
					.align(Alignment.CenterVertically),
			)

			val isChildItemSynced by childItemViewModel.isSynced.collectAsState()
			SyncButton(
				isActive = isChildItemSynced,
				modifier = Modifier
					.fillMaxWidth()
					.clickable { childItemViewModel.toggleSync() }
					.weight(1f)
					.align(Alignment.CenterVertically),
			)

			Image(
				painter = painterResource(id = R.drawable.av_shuffle),
				contentDescription = stringResource(id = R.string.btn_shuffle_files),
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.clickable(onClick = childItemViewModel::playShuffled)
					.align(Alignment.CenterVertically),
			)
		}
	}

	@Composable
	fun RenderTrackHeaderItem(position: Int, serviceFile: ServiceFile) {
		val fileItemViewModel = remember(trackHeadlineViewModelProvider::getViewModel)

		DisposableEffect(serviceFile) {
			fileItemViewModel.promiseUpdate(files, position)

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
		val fileName by fileItemViewModel.title.collectAsState()
		val isPlaying by remember { derivedStateOf { playingFile?.serviceFile == serviceFile } }

		TrackHeaderItemView(
			itemName = fileName,
			isActive = isPlaying,
			isHiddenMenuShown = isMenuShown,
			onItemClick = fileItemViewModel::viewFileDetails,
			onHiddenMenuClick = {
				itemListMenuBackPressedHandler.hideAllMenus()
				fileItemViewModel.showMenu()
			},
			onAddToNowPlayingClick = fileItemViewModel::addToNowPlaying,
			onViewFilesClick = fileItemViewModel::viewFileDetails,
			onPlayClick = {
				fileItemViewModel.hideMenu()
				fileListViewModel.play(position)
			}
		)
	}

	@Composable
	fun BoxWithConstraintsScope.LoadedItemListView() {
		val items by itemListViewModel.items.collectAsState()

		val lazyListState = rememberLazyListState() // instantiate lazyListState here to ensure it updates when a new list is retrieved

		val knobHeight by remember {
			derivedStateOf {
				lazyListState.layoutInfo.totalItemsCount
					.takeIf { it > 0 }
					?.let { maxHeight / (rowHeight * it) }
					?.takeIf { it > 0 && it < 1 }
			}
		}

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

				itemsIndexed(items) { i, f ->
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

	val systemUiController = rememberSystemUiController()
	systemUiController.setStatusBarColor(MaterialTheme.colors.surface)

	Surface {
		val toolbarState = rememberCollapsingToolbarScaffoldState()
		val headerHidingProgress by remember { derivedStateOf { 1 - toolbarState.toolbarState.progress } }

		CollapsingToolbarScaffold(
			enabled = true,
			state = toolbarState,
			scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
			modifier = Modifier.fillMaxSize(),
			toolbar = {
				val appBarHeight = 56
				val topPadding by remember { derivedStateOf { (appBarHeight - 46 * headerHidingProgress).dp } }
				val expandedTitleHeight = 84
				val expandedIconSize = 36
				val expandedMenuVerticalPadding = 12
				val boxHeight =
					expandedTitleHeight + expandedIconSize + expandedMenuVerticalPadding * 2 + appBarHeight
				BoxWithConstraints(
					modifier = Modifier
						.height(boxHeight.dp)
						.padding(top = topPadding)
				) {
					val minimumMenuWidth = (3 * 32).dp
					val acceleratedProgress by remember {
						derivedStateOf {
							1 - toolbarState.toolbarState.progress.pow(
								3
							).coerceIn(0f, 1f)
						}
					}
					ProvideTextStyle(MaterialTheme.typography.h5) {
						val startPadding by remember { derivedStateOf { (4 + 48 * headerHidingProgress).dp } }
						val endPadding by remember { derivedStateOf { 4.dp + minimumMenuWidth * acceleratedProgress } }
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
							)
						}
					}

					val menuWidth by remember { derivedStateOf { (maxWidth - (maxWidth - minimumMenuWidth) * acceleratedProgress) } }
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
						val iconSize by remember { derivedStateOf { (expandedIconSize - (12 * headerHidingProgress)).dp } }

						Image(
							painter = painterResource(id = R.drawable.av_play),
							contentDescription = stringResource(id = R.string.btn_play),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f)
								.size(iconSize)
								.clickable {
									fileListViewModel.play()
								}
						)

						val isSynced by itemListViewModel.isSynced.collectAsState()

						SyncButton(
							isActive = isSynced,
							modifier = Modifier
								.fillMaxWidth()
								.size(iconSize)
								.clickable { itemListViewModel.toggleSync() }
								.weight(1f),
						)

						Image(
							painter = painterResource(id = R.drawable.av_shuffle),
							contentDescription = stringResource(id = R.string.btn_shuffle_files),
							modifier = Modifier
								.fillMaxWidth()
								.size(iconSize)
								.weight(1f)
								.clickable {
									fileListViewModel.playShuffled()
								}
						)
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
				val isFilesLoaded by fileListViewModel.isLoaded.collectAsState()
				val isLoaded = !isItemsLoading && isFilesLoaded

				if (isLoaded) LoadedItemListView()
				else CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}
		}
	}
}
