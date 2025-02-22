package com.lasthopesoftware.bluewater.client.browsing.items.list

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledPlayButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.LabelledShuffleButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.TrackTitleItemView
import com.lasthopesoftware.bluewater.client.browsing.files.list.UnlabelledPlayButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.UnlabelledShuffleButton
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledActiveDownloadsButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledRefreshButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.LabelledSearchButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.MoreFileOptionsMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.MoreItemsOnlyOptionsMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.UnlabelledActiveDownloadsButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.UnlabelledRefreshButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.UnlabelledSearchButton
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncIcon
import com.lasthopesoftware.bluewater.shared.android.ui.components.BackButton
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
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.expandedMenuVerticalPadding
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.expandedTitleHeight
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.rowScrollPadding
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions.topMenuIconSize
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import kotlin.math.pow
import kotlin.math.roundToInt

private val appBarHeight = Dimensions.appBarHeight
private val minimumMenuWidth = (topMenuIconSize + Dimensions.viewPaddingUnit * 2) * 3

private val expandedIconSize = Dimensions.menuHeight
private val boxHeight = expandedTitleHeight + appBarHeight + expandedIconSize + expandedMenuVerticalPadding * 2

@Composable
fun ItemsCountHeader(itemsCount: Int) {
	Box(
		modifier = Modifier
			.padding(Dimensions.viewPaddingUnit)
			.height(Dimensions.menuHeight)
	) {
		ProvideTextStyle(MaterialTheme.typography.h5) {
			Text(
				text = stringResource(R.string.item_count_label, itemsCount),
				fontWeight = FontWeight.Bold,
				modifier = Modifier
					.padding(Dimensions.viewPaddingUnit)
					.align(Alignment.CenterStart)
			)
		}
	}
}

@Composable
fun FilesCountHeader(filesCount: Int) {
	Box(
		modifier = Modifier
			.padding(Dimensions.viewPaddingUnit)
			.height(Dimensions.menuHeight)
	) {
		ProvideTextStyle(MaterialTheme.typography.h5) {
			Text(
				text = stringResource(R.string.file_count_label, filesCount),
				fontWeight = FontWeight.Bold,
				modifier = Modifier
					.padding(Dimensions.viewPaddingUnit)
					.align(Alignment.CenterStart)
			)
		}
	}
}

@Composable
private fun CollapsedItemListMenu(
	itemListViewModel: ItemListViewModel,
	fileListViewModel: FileListViewModel,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	modifier: Modifier = Modifier
) {
	Row(modifier = modifier) {
		val files by fileListViewModel.files.subscribeAsState()
		if (files.any()) {
			UnlabelledPlayButton(
				libraryState = itemListViewModel,
				playbackServiceController = playbackServiceController,
				serviceFilesListState = fileListViewModel
			)

			UnlabelledShuffleButton(
				libraryState = itemListViewModel,
				playbackServiceController = playbackServiceController,
				serviceFilesListState = fileListViewModel
			)

			UnlabelledRefreshButton(itemListViewModel, fileListViewModel)
		} else {
			UnlabelledActiveDownloadsButton(
				itemListViewModel = itemListViewModel,
				applicationNavigation = applicationNavigation
			)

			UnlabelledSearchButton(
				itemListViewModel = itemListViewModel,
				applicationNavigation = applicationNavigation
			)

			UnlabelledRefreshButton(itemListViewModel, fileListViewModel)
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
				applicationNavigation.viewFileDetails(it, fileListViewModel.files.value, position)
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
								playbackLibraryItems.playItem(it, ItemId(item.key))
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
							playbackLibraryItems.playItemShuffled(it, ItemId(item.key))
						}
					})
					.align(Alignment.CenterVertically),
			)
		}
	}
}

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
	val files by fileListViewModel.files.subscribeAsState()
	val rowHeight = Dimensions.standardRowHeight
	val itemValue by itemListViewModel.itemValue.collectAsState()

	val lazyListState = rememberLazyListState()

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
			item(contentType = ItemListContentType.Spacer) {
				Spacer(
					modifier = Modifier
						.requiredHeight(headerHeight)
						.fillMaxWidth()
				)
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
						playbackLibraryItems
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
					playbackServiceController
				)

				if (i < files.lastIndex)
					Divider()
			}
		}
	}

	val isFilesLoading by fileListViewModel.isLoading.subscribeAsState()

	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		ControlSurface {

			val isItemsLoading by itemListViewModel.isLoading.subscribeAsState()

			val collapsedHeight = appBarHeight

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
						BackButton(
							applicationNavigation::navigateUp,
							modifier = Modifier
								.align(Alignment.TopStart)
								.padding(Dimensions.topRowOuterPadding)
						)

						val moreMenuModifier = Modifier
							.align(Alignment.TopEnd)
							.padding(
								vertical = Dimensions.topRowOuterPadding,
								horizontal = Dimensions.viewPaddingUnit
							)

						if (files.any()) MoreFileOptionsMenu(
							fileListViewModel,
							moreMenuModifier
						) else MoreItemsOnlyOptionsMenu(
							itemListViewModel,
							applicationNavigation,
							moreMenuModifier
						)

						val headerCollapseProgress by heightScaler.getProgressState()
						val topPadding by remember { derivedStateOf { linearInterpolation(appBarHeight, 14.dp, headerCollapseProgress) } }
						val endPadding by remember {
							derivedStateOf {
								linearInterpolation(
									Dimensions.viewPaddingUnit * 2,
									topMenuIconSize + Dimensions.viewPaddingUnit,
									headerCollapseProgress
								)
							}
						}
						BoxWithConstraints(
							modifier = Modifier
								.align(Alignment.TopCenter)
								.padding(top = topPadding, end = endPadding),
						) nestedBoxScope@{
							val acceleratedToolbarStateProgress by remember {
								derivedStateOf {
									(1 - headerCollapseProgress).pow(3).coerceIn(0f, 1f)
								}
							}

							val acceleratedHeaderHidingProgress by remember {
								derivedStateOf { 1 - acceleratedToolbarStateProgress }
							}

							ProvideTextStyle(MaterialTheme.typography.h5) {
								val startPadding by remember {
									derivedStateOf {
										linearInterpolation(
											Dimensions.viewPaddingUnit,
											Dimensions.topMenuIconSizeWithPadding,
											headerCollapseProgress
										)
									}
								}

								val textEndPadding by remember {
									derivedStateOf {
										linearInterpolation(
											Dimensions.viewPaddingUnit,
											Dimensions.viewPaddingUnit + minimumMenuWidth,
											acceleratedHeaderHidingProgress
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
											.padding(start = startPadding, end = textEndPadding),
									)
								} else {
									MarqueeText(
										text = itemValue,
										overflow = TextOverflow.Ellipsis,
										gradientSides = setOf(GradientSide.End),
										gradientEdgeColor = MaterialTheme.colors.surface,
										modifier = Modifier
											.fillMaxWidth()
											.padding(start = startPadding, end = textEndPadding),
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

							val expandedTopRowPadding = expandedTitleHeight + expandedMenuVerticalPadding
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
											bottom = expandedMenuVerticalPadding,
											start = Dimensions.viewPaddingUnit * 2
										)
										.width(menuWidth)
										.align(Alignment.TopEnd),
									horizontalArrangement = Arrangement.SpaceEvenly,
								) {
									if (files.any()) {
										LabelledPlayButton(
											libraryState = itemListViewModel,
											playbackServiceController = playbackServiceController,
											serviceFilesListState = fileListViewModel,
											modifier = textModifier,
										)

										LabelledShuffleButton(
											libraryState = itemListViewModel,
											playbackServiceController = playbackServiceController,
											serviceFilesListState = fileListViewModel,
											modifier = textModifier
										)

										LabelledRefreshButton(
											itemListViewModel,
											fileListViewModel,
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

										LabelledRefreshButton(
											itemListViewModel,
											fileListViewModel,
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
									modifier = Modifier
										.padding(
											top = topRowPadding,
											bottom = expandedMenuVerticalPadding,
											start = Dimensions.viewPaddingUnit * 2
										)
										.width(menuWidth)
										.align(Alignment.TopEnd),
								)
							}
						}
					}
				} else {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.height(collapsedHeight),
						contentAlignment = Alignment.CenterStart,
					) {
						BackButton(
							applicationNavigation::navigateUp,
							modifier = Modifier.padding(horizontal = Dimensions.topRowOuterPadding)
						)

						ProvideTextStyle(MaterialTheme.typography.h5) {
							MarqueeText(
								text = itemValue,
								overflow = TextOverflow.Ellipsis,
								gradientSides = setOf(GradientSide.End),
								gradientEdgeColor = MaterialTheme.colors.surface,
								modifier = Modifier
									.fillMaxWidth()
									.padding(
										start = Dimensions.topMenuIconSizeWithPadding,
										end = Dimensions.viewPaddingUnit + minimumMenuWidth
									),
								isMarqueeEnabled = !lazyListState.isScrollInProgress
							)
						}

						if (!isFilesLoading && !isItemsLoading) {
							if (files.any()) MoreFileOptionsMenu(
								fileListViewModel,
								Modifier
									.align(Alignment.CenterEnd)
									.padding(
										horizontal = Dimensions.viewPaddingUnit
									)
							) else MoreItemsOnlyOptionsMenu(
								itemListViewModel,
								applicationNavigation,
								Modifier
									.align(Alignment.CenterEnd)
									.padding(
										horizontal = Dimensions.viewPaddingUnit
									)
							)

							val menuWidth = minimumMenuWidth

							CollapsedItemListMenu(
								itemListViewModel = itemListViewModel,
								fileListViewModel = fileListViewModel,
								applicationNavigation = applicationNavigation,
								playbackServiceController = playbackServiceController,
								modifier = Modifier
									.padding(
										start = Dimensions.viewPaddingUnit * 2,
										end = topMenuIconSize + Dimensions.viewPaddingUnit
									)
									.width(menuWidth)
									.align(Alignment.CenterEnd),
							)
						}
					}
				}
			}
		}
	}
}
