package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.NowPlayingItemView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.PlayPauseButton
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.shared.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.DragDropItemScope
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.DragDropLazyColumn
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.getVisibleItemInfoFor
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.rememberDragDropListState
import com.lasthopesoftware.bluewater.shared.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import kotlinx.coroutines.launch

private val controlRowHeight = 72.dp
private const val playlistControlAlpha = .8f

private class ScreenDimensionsScope(val screenHeight: Dp, val screenWidth: Dp, innerBoxScope: BoxWithConstraintsScope)
	: BoxWithConstraintsScope by innerBoxScope

@Composable
private fun NowPlayingCoverArtView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
) {
	Box(
		modifier = Modifier.fillMaxSize()
	) {
		val defaultImage by nowPlayingCoverArtViewModel.defaultImage.collectAsState()
		defaultImage
			?.let {
				val defaultImageBitmap by remember { derivedStateOf { it.asImageBitmap() } }
				Image(
					bitmap = defaultImageBitmap,
					contentDescription = stringResource(id = R.string.img_now_playing_loading),
					contentScale = ContentScale.Crop,
					alignment = Alignment.Center,
					modifier = Modifier.fillMaxSize(),
				)
			}

		val isLoadingImage by nowPlayingCoverArtViewModel.isNowPlayingImageLoading.collectAsState()
		if (isLoadingImage) {
			CircularProgressIndicator(
				modifier = Modifier.align(Alignment.Center)
			)
		} else {
			val coverArt by nowPlayingCoverArtViewModel.nowPlayingImage.collectAsState()
			val coverArtBitmap by remember { derivedStateOf { coverArt?.asImageBitmap() } }
			coverArtBitmap
				?.let {
					Image(
						bitmap = it,
						contentDescription = stringResource(id = R.string.img_now_playing),
						contentScale = ContentScale.Crop,
						alignment = Alignment.Center,
						modifier = Modifier.fillMaxSize(),
					)
				}
		}
	}
}

@Composable
private fun KeepScreenOn(keepScreenOn: Boolean) {
	val currentView = LocalView.current
	DisposableEffect(keepScreenOn) {
		currentView.keepScreenOn = keepScreenOn
		onDispose {
			currentView.keepScreenOn = false
		}
	}
}

@Composable
private fun NowPlayingProgressIndicator(nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel, modifier: Modifier = Modifier) {
	val filePosition by nowPlayingFilePropertiesViewModel.filePosition.subscribeAsState()
	val fileDuration by nowPlayingFilePropertiesViewModel.fileDuration.subscribeAsState()
	val fileProgress by remember { derivedStateOf { filePosition / fileDuration.toFloat() } }

	LinearProgressIndicator(
		progress = fileProgress,
		color = Color.White,
		backgroundColor = Color.White.copy(alpha = .2f),
		modifier = modifier
	)
}

@Composable
private fun NowPlayingHeadline(modifier: Modifier = Modifier, nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel) {
	Column(modifier = modifier) {
		ProvideTextStyle(value = MaterialTheme.typography.h5) {
			val title by nowPlayingFilePropertiesViewModel.title.subscribeAsState()

			MarqueeText(
				text = title,
				gradientEdgeColor = Color.Transparent,
			)
		}

		ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
			val artist by nowPlayingFilePropertiesViewModel.artist.subscribeAsState()
			MarqueeText(
				text = artist,
				gradientEdgeColor = Color.Transparent,
			)
		}
	}
}

@Composable
private fun PlaylistControls(
	modifier: Modifier = Modifier,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playlistViewModel: NowPlayingPlaylistViewModel,
	playbackServiceController: ControlPlaybackService,
	onHide: (() -> Unit)? = null,
) {
	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.SpaceAround,
		verticalAlignment = Alignment.CenterVertically,
	) {
		val isEditingPlaylist by playlistViewModel.isEditingPlaylistState.subscribeAsState()
		if (isEditingPlaylist) {
			Image(
				painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
				contentDescription = stringResource(id = R.string.finish_edit_now_playing_list),
				modifier = Modifier.clickable {
					playlistViewModel.finishPlaylistEdit()
				},
				alpha = playlistControlAlpha,
			)
		} else {
			Image(
				painter = painterResource(id = R.drawable.pencil),
				contentDescription = stringResource(id = R.string.edit_now_playing_list),
				modifier = Modifier.clickable {
					playlistViewModel.editPlaylist()
				},
				alpha = playlistControlAlpha,
			)
		}

		if (isEditingPlaylist) {
			Image(
				painter = painterResource(id = R.drawable.clear_all_white_36dp),
				contentDescription = stringResource(R.string.empty_playlist),
				modifier = Modifier.clickable(onClick = playlistViewModel::requestPlaylistClearingPermission),
				alpha = playlistControlAlpha,
			)
		} else {
			val isRepeating by playlistViewModel.isRepeating.subscribeAsState()
			if (isRepeating) {
				Image(
					painter = painterResource(id = R.drawable.av_repeat_white),
					contentDescription = stringResource(id = R.string.btn_complete_playlist),
					modifier = Modifier.clickable {
						playlistViewModel.toggleRepeating()
					},
					alpha = playlistControlAlpha,
				)
			} else {
				Image(
					painter = painterResource(id = R.drawable.av_no_repeat_white),
					contentDescription = stringResource(id = R.string.btn_repeat_playlist),
					modifier = Modifier.clickable {
						playlistViewModel.toggleRepeating()
					},
					alpha = playlistControlAlpha,
				)
			}
		}

		if (isEditingPlaylist) {
			Image(
				painter = painterResource(id = R.drawable.upload_36dp),
				contentDescription = stringResource(id = R.string.save_playlist),
				modifier = Modifier.clickable {
					playlistViewModel.enableSavingPlaylist()
				},
				alpha = playlistControlAlpha,
			)
		} else {
			PlayPauseButton(
				nowPlayingFilePropertiesViewModel,
				playbackServiceController,
				alpha = playlistControlAlpha
			)
		}

		if (onHide != null) {
			Image(
				painter = painterResource(R.drawable.chevron_up_white_36dp),
				alpha = playlistControlAlpha,
				contentDescription = stringResource(R.string.btn_hide_files),
				modifier = Modifier
					.clickable(onClick = {
						playlistViewModel.finishPlaylistEdit()
						onHide()
					})
					.rotate(180f),
			)
		}
	}
}

private object ConsumeAllVerticalFlingScrollConnection : NestedScrollConnection {

	override fun onPostScroll(
		consumed: Offset,
		available: Offset,
		source: NestedScrollSource
	): Offset {
		return when (source) {
			NestedScrollSource.Fling -> available.copy(x = 0f)
			else -> Offset.Zero
		}
	}

	override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available.copy(x = 0f)
}

@Composable
private fun NowPlayingRating(nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.height(controlRowHeight),
		verticalArrangement = Arrangement.Center,
	) {
		val rating by nowPlayingFilePropertiesViewModel.songRating.subscribeAsState()
		val ratingInt by remember { derivedStateOf { rating.toInt() } }
		val isRatingEnabled by nowPlayingFilePropertiesViewModel.isSongRatingEnabled.subscribeAsState()
		RatingBar(
			rating = ratingInt,
			color = Color.White,
			backgroundColor = Color.White.copy(alpha = .1f),
			modifier = Modifier
				.fillMaxWidth()
				.height(Dimensions.menuHeight),
			onRatingSelected = if (isRatingEnabled) {
				{
					nowPlayingFilePropertiesViewModel.updateRating(it.toFloat())
				}
			} else null
		)

		val isReadOnly by nowPlayingFilePropertiesViewModel.isReadOnly.subscribeAsState()
		if (isReadOnly) {
			ProvideTextStyle(value = MaterialTheme.typography.caption) {
				Text(
					text = stringResource(id = R.string.readOnlyConnection)
				)
			}
		}
	}
}

@Composable
private fun NowPlayingPlaybackControls(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	modifier: Modifier = Modifier,
) {
	Row(
		modifier = modifier.height(controlRowHeight),
		verticalAlignment = Alignment.CenterVertically,
	) {
		val activeLibraryId by nowPlayingFilePropertiesViewModel.activeLibraryId.subscribeAsState()
		Image(
			painter = painterResource(id = R.drawable.av_previous_white),
			contentDescription = stringResource(id = R.string.btn_previous),
			modifier = Modifier
				.weight(1f)
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null
				) {
					activeLibraryId?.also(playbackServiceController::previous)
				},
		)

		PlayPauseButton(
			nowPlayingFilePropertiesViewModel,
			playbackServiceController,
			modifier = Modifier.weight(1f),
		)

		Image(
			painter = painterResource(id = R.drawable.av_next_white),
			contentDescription = stringResource(id = R.string.btn_next),
			modifier = Modifier
				.weight(1f)
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null
				) {
					activeLibraryId?.also(playbackServiceController::next)
				}
		)
	}
}

@Composable
fun NowPlayingControls(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	modifier: Modifier = Modifier,
) {
	Column(modifier = modifier) {
		NowPlayingRating(nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel)

		Spacer(modifier = Modifier.height(ProgressIndicatorDefaults.StrokeWidth))

		NowPlayingPlaybackControls(
			nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
			playbackServiceController = playbackServiceController,
		)
	}
}

@Composable
fun NowPlayingPlaylist(
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	playlistViewModel: NowPlayingPlaylistViewModel,
	modifier: Modifier = Modifier,
) {
	val nowPlayingFiles by playlistViewModel.nowPlayingList.subscribeAsState()
	val playlist by remember { derivedStateOf { nowPlayingFiles.map { p -> p.serviceFile } } }
	val activeLibraryId by nowPlayingFilePropertiesViewModel.activeLibraryId.subscribeAsState()

	val dragDropListState = rememberDragDropListState(
		onMove = { from, to ->
			playlistViewModel.swapFiles(from, to)
		},
		onDragEnd = { from, to ->
			activeLibraryId?.also {
				playbackServiceController.moveFile(it, from, to)
			}
		}
	)

	val playingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.subscribeAsState()

	val isAutoScrollEnabled by playlistViewModel.isAutoScrolling.subscribeAsState()
	if (isAutoScrollEnabled) {
		LaunchedEffect(key1 = playingFile) {
			playingFile?.apply {
				val listState = dragDropListState.lazyListState
				if (!listState.isScrollInProgress)
					listState.scrollToItem(playlistPosition)
			}
		}
	}

	@Composable
	fun DragDropItemScope.NowPlayingFileView(positionedFile: PositionedFile) {
		val fileItemViewModel = remember(childItemViewModelProvider::getViewModel)

		DisposableEffect(activeLibraryId, positionedFile) {
			activeLibraryId?.also {
				fileItemViewModel.promiseUpdate(it, positionedFile.serviceFile)
			}

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val isMenuShown by fileItemViewModel.isMenuShown.collectAsState()
		val fileName by fileItemViewModel.title.collectAsState()
		val artist by fileItemViewModel.artist.collectAsState()
		val isPlaying by remember { derivedStateOf { playingFile == positionedFile } }

		val viewFilesClickHandler = {
			activeLibraryId?.also {
				applicationNavigation.viewFileDetails(
					it,
					playlist,
					positionedFile.playlistPosition
				)
			}
			Unit
		}

		val isEditingPlaylist by playlistViewModel.isEditingPlaylistState.subscribeAsState()
		NowPlayingItemView(
			itemName = fileName,
			artist = artist,
			isActive = isPlaying,
			isEditingPlaylist = isEditingPlaylist,
			isHiddenMenuShown = isMenuShown,
			onItemClick = viewFilesClickHandler,
			dragDropListState = dragDropListState,
			onHiddenMenuClick = {
				if (!isEditingPlaylist) {
					itemListMenuBackPressedHandler.hideAllMenus()
					fileItemViewModel.showMenu()
				}
			},
			onRemoveFromNowPlayingClick = {
				activeLibraryId?.also {
					playbackServiceController
						.removeFromPlaylistAtPosition(it, positionedFile.playlistPosition)
				}
			},
			onViewFilesClick = viewFilesClickHandler,
			onPlayClick = {
				fileItemViewModel.hideMenu()
				activeLibraryId?.also {
					playbackServiceController.seekTo(it, positionedFile.playlistPosition)
				}
			}
		)
	}

	DragDropLazyColumn(
		dragDropListState = dragDropListState,
		modifier = modifier,
	) {
		dragDropItems(items = nowPlayingFiles, keyFactory = { _, f -> f }) { _, f ->
			NowPlayingFileView(positionedFile = f)
		}
	}
}

private val collapsedControlsHeight = ProgressIndicatorDefaults.StrokeWidth + Dimensions.appBarHeight
private val expandedControlsHeight = controlRowHeight + collapsedControlsHeight

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun BoxWithConstraintsScope.NowPlayingNarrowView(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	screenOnState: NowPlayingScreenViewModel,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
) {
	val pagerState = rememberLazyListState()
	val isSettledOnFirstPage by remember { derivedStateOf { pagerState.firstVisibleItemIndex == 0 && pagerState.firstVisibleItemScrollOffset == 0 } }
	val isNotSettledOnFirstPage by remember { derivedStateOf { !isSettledOnFirstPage } }
	val isEditingPlaylist by playlistViewModel.isEditingPlaylistState.subscribeAsState()

	val scope = rememberCoroutineScope()
	BackHandler(isNotSettledOnFirstPage) {
		when {
			itemListMenuBackPressedHandler.hideAllMenus() -> {}
			isEditingPlaylist -> playlistViewModel.finishPlaylistEdit()
			isNotSettledOnFirstPage -> {
				playlistViewModel.finishPlaylistEdit()
				scope.launch { pagerState.animateScrollToItem(0) }
			}
		}
	}

	val isScreenControlsVisible by nowPlayingFilePropertiesViewModel.isScreenControlsVisible.subscribeAsState()

	val filePropertiesHeight = maxHeight - expandedControlsHeight

	val filePropertiesHeightPx = LocalDensity.current.run { filePropertiesHeight.toPx() }

	val firstPageShownProgress by remember {
		derivedStateOf {
			pagerState.layoutInfo.visibleItemsInfo
				.getVisibleItemInfoFor(0)
				?.run {
					(filePropertiesHeightPx + offset) / filePropertiesHeightPx
				}
				?.coerceIn(0f, 1f)
				?: 0f
		}
	}

	val snappingLayout = remember(pagerState) { SnapLayoutInfoProvider(pagerState) { _, _, _, _, _ -> 0 } }

	CompositionLocalProvider(
		LocalOverscrollConfiguration provides null
	) {
		LazyColumn(
			flingBehavior = rememberSnapFlingBehavior(snappingLayout),
			state = pagerState,
			userScrollEnabled = !isEditingPlaylist,
		) {
			item {
				Box(
					modifier = Modifier
						.height(filePropertiesHeight)
						.nestedScroll(ConsumeAllVerticalFlingScrollConnection)
						.clickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = { nowPlayingFilePropertiesViewModel.showNowPlayingControls() }
						),
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						NowPlayingHeadline(modifier = Modifier.weight(1f), nowPlayingFilePropertiesViewModel)

						if (isScreenControlsVisible) {
							Row(modifier = Modifier.wrapContentWidth()) {
								val isScreenOnEnabled by screenOnState.isScreenOnEnabled.collectAsState()
								Image(
									painter = painterResource(if (isScreenOnEnabled) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp),
									alpha = .8f,
									contentDescription = stringResource(if (isScreenOnEnabled) R.string.screen_is_on else R.string.screen_is_off),
									modifier = Modifier
										.padding(Dimensions.viewPaddingUnit)
										.clickable(onClick = screenOnState::toggleScreenOn),
								)

								Image(
									painter = painterResource(R.drawable.chevron_up_white_36dp),
									alpha = .8f,
									contentDescription = stringResource(R.string.btn_view_files),
									modifier = Modifier
										.padding(Dimensions.viewPaddingUnit)
										.clickable(onClick = {
											scope.launch {
												pagerState.animateScrollToItem(1)
											}
										}),
								)
							}
						}
					}
				}
			}

			item {
				Column(
					modifier = Modifier
						.nestedScroll(ConsumeAllVerticalFlingScrollConnection)
						.fillMaxSize()
						.height(maxHeight),
				) {
					val controlsHeight by remember {
						derivedStateOf {
							linearInterpolation(
								initial = collapsedControlsHeight,
								final = expandedControlsHeight,
								firstPageShownProgress
							)
						}
					}

					Column(
						modifier = Modifier
							.fillMaxWidth()
							.height(controlsHeight),
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Box(
							modifier = Modifier.height(Dimensions.appBarHeight),
							contentAlignment = Alignment.Center,
						) {
							if (isNotSettledOnFirstPage) {
								PlaylistControls(
									modifier = Modifier
										.alpha(1 - firstPageShownProgress)
										.fillMaxWidth(),
									nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
									playlistViewModel = playlistViewModel,
									playbackServiceController = playbackServiceController,
									onHide = {
										scope.launch {
											pagerState.animateScrollToItem(0)
										}
									}
								)
							}
						}

						NowPlayingProgressIndicator(nowPlayingFilePropertiesViewModel, modifier = Modifier.fillMaxWidth())
					}

					if (isSettledOnFirstPage) {
						playlistViewModel.autoScroll()
					} else {
						playlistViewModel.manualScroll()
					}

					CompositionLocalProvider(
						LocalOverscrollConfiguration provides OverscrollConfiguration()
					) {
						NowPlayingPlaylist(
							childItemViewModelProvider,
							nowPlayingFilePropertiesViewModel,
							applicationNavigation,
							playbackServiceController,
							itemListMenuBackPressedHandler,
							playlistViewModel,
							modifier = Modifier
								.background(SharedColors.overlayDark)
								.fillMaxHeight()
						)
					}
				}
			}
		}
	}

	if (isSettledOnFirstPage && isScreenControlsVisible) {
		NowPlayingControls(
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.fillMaxWidth(),
			nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
			playbackServiceController = playbackServiceController,
		)
	}
}

@Composable
private fun ScreenDimensionsScope.NowPlayingWideView(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	screenOnState: NowPlayingScreenViewModel,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
) {
	val isEditingPlaylist by playlistViewModel.isEditingPlaylistState.subscribeAsState()
	BackHandler(itemListMenuBackPressedHandler.hideAllMenus() || isEditingPlaylist) {
		if (isEditingPlaylist) playlistViewModel.finishPlaylistEdit()
	}

	Row(
		modifier = Modifier.fillMaxSize(),
	) {
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.weight(1f, fill = true)
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = { nowPlayingFilePropertiesViewModel.showNowPlayingControls() }
				)
		) {
			val isScreenControlsVisible by nowPlayingFilePropertiesViewModel.isScreenControlsVisible.subscribeAsState()
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				NowPlayingHeadline(modifier = Modifier.weight(1f), nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel)

				if (isScreenControlsVisible) {
					val isScreenOnEnabled by screenOnState.isScreenOnEnabled.collectAsState()
					Image(
						painter = painterResource(if (isScreenOnEnabled) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp),
						alpha = .8f,
						contentDescription = stringResource(if (isScreenOnEnabled) R.string.screen_is_on else R.string.screen_is_off),
						modifier = Modifier
							.padding(Dimensions.viewPaddingUnit)
							.clickable(onClick = screenOnState::toggleScreenOn),
					)
				}
			}

			if (isScreenControlsVisible) {
				NowPlayingControls(
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.fillMaxWidth(),
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					playbackServiceController = playbackServiceController,
				)
			}

			NowPlayingProgressIndicator(
				nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter)
					.padding(bottom = controlRowHeight)
			)
		}

		val screenWidth = this@NowPlayingWideView.screenWidth
		Column(
			modifier = Modifier
				.fillMaxHeight()
				.width(this@NowPlayingWideView.screenHeight.coerceAtMost(screenWidth / 2))
				.background(SharedColors.overlayDark),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			PlaylistControls(
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimensions.appBarHeight),
				nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
				playlistViewModel = playlistViewModel,
				playbackServiceController = playbackServiceController,
			)

			DisposableEffect(key1 = Unit) {
				playlistViewModel.autoScroll()

				onDispose { playlistViewModel.manualScroll() }
			}

			NowPlayingPlaylist(
				childItemViewModelProvider,
				nowPlayingFilePropertiesViewModel,
				applicationNavigation,
				playbackServiceController,
				itemListMenuBackPressedHandler,
				playlistViewModel,
				modifier = Modifier.fillMaxHeight()
			)
		}
	}
}

@Composable
fun NowPlayingView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	screenOnState: NowPlayingScreenViewModel,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	connectionWatcherViewModel: ConnectionWatcherViewModel
) {
	val isScreenOn by screenOnState.isScreenOn.collectAsState()
	KeepScreenOn(isScreenOn)

	ControlSurface(
		color = Color.Transparent,
		contentColor = Color.White,
		controlColor = Color.White,
	) {
		NowPlayingCoverArtView(nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel)

		val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

		// Nest boxes to get proper size constraints
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxSize()
				.background(SharedColors.overlayDark)
				.padding(systemBarsPadding),
		) {
			val screenScope by remember {
				derivedStateOf {
					with (this@BoxWithConstraints) {
						ScreenDimensionsScope(
							maxHeight + systemBarsPadding.calculateTopPadding() + systemBarsPadding.calculateBottomPadding(),
							maxWidth  + systemBarsPadding.calculateLeftPadding(LayoutDirection.Ltr) + systemBarsPadding.calculateRightPadding(LayoutDirection.Ltr),
							this
						)
					}
				}
			}

			with (screenScope) {
				if (screenWidth < screenHeight) {
					NowPlayingNarrowView(
						nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
						screenOnState = screenOnState,
						playbackServiceController = playbackServiceController,
						playlistViewModel = playlistViewModel,
						childItemViewModelProvider = childItemViewModelProvider,
						applicationNavigation = applicationNavigation,
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					)
				} else {
					NowPlayingWideView(
						nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
						screenOnState = screenOnState,
						playbackServiceController = playbackServiceController,
						playlistViewModel = playlistViewModel,
						childItemViewModelProvider = childItemViewModelProvider,
						applicationNavigation = applicationNavigation,
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					)
				}
			}
		}
	}

	val isSavingPlaylistActive by playlistViewModel.isSavingPlaylistActive.subscribeAsState()
	if (isSavingPlaylistActive) {
		Dialog(
			onDismissRequest = { playlistViewModel.disableSavingPlaylist() },
		) {
			val selectedPlaylistPath by playlistViewModel.selectedPlaylistPath.subscribeAsState()

			BackHandler(selectedPlaylistPath.isNotEmpty()) {
				playlistViewModel.updateSelectedPlaylistPath("")
			}

			ControlSurface {
				Column(
					modifier = Modifier
						.padding(Dimensions.viewPaddingUnit * 2)
						.fillMaxWidth()
						.fillMaxHeight(.8f),
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(bottom = Dimensions.viewPaddingUnit * 4)
					) {
						ProvideTextStyle(MaterialTheme.typography.h5) {
							Text(
								text = stringResource(id = R.string.save_playlist),
								modifier = Modifier
									.weight(1f)
									.align(Alignment.CenterVertically),
							)
						}

						Icon(
							painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
							contentDescription = stringResource(id = R.string.btn_cancel),
							modifier = Modifier
								.clickable { playlistViewModel.disableSavingPlaylist() }
								.align(Alignment.CenterVertically),
						)
					}

					TextField(
						value = selectedPlaylistPath,
						onValueChange = playlistViewModel::updateSelectedPlaylistPath,
						modifier = Modifier.fillMaxWidth(),
						placeholder = { Text(stringResource(R.string.new_or_existing_playlist_path)) },
						singleLine = true,
					)

					val filteredPlaylistPaths by playlistViewModel.filteredPlaylistPaths.subscribeAsState()

					Box(
						modifier = Modifier
							.fillMaxWidth()
							.weight(1f)
					) {
						ProvideTextStyle(value = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal)) {
							LazyColumn(
								modifier = Modifier.fillMaxWidth()
							) {
								items(filteredPlaylistPaths) { playlistPath ->
									Row(
										modifier = Modifier
											.height(Dimensions.standardRowHeight)
											.fillMaxWidth()
											.clickable {
												playlistViewModel.updateSelectedPlaylistPath(
													playlistPath
												)
											},
										verticalAlignment = Alignment.CenterVertically,
									) {
										Text(text = playlistPath)
									}
								}
							}
						}
					}

					val isPlaylistPathValid by playlistViewModel.isPlaylistPathValid.subscribeAsState()

					if (isPlaylistPathValid) {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(Dimensions.viewPaddingUnit)
						) {
							Icon(
								painter = painterResource(id = R.drawable.ic_save_white_36dp),
								contentDescription = stringResource(id = R.string.save),
								modifier = Modifier
									.fillMaxWidth()
									.weight(1f)
									.clickable { playlistViewModel.savePlaylist() }
									.align(Alignment.CenterVertically),
							)
						}
					}
				}
			}
		}
	}

	val isConnectionLost by connectionWatcherViewModel.isCheckingConnection.collectAsState()
	if (isConnectionLost) {
		AlertDialog(
			onDismissRequest = { connectionWatcherViewModel.cancelLibraryConnectionPolling() },
			title = { Text(text = stringResource(id = R.string.lbl_connection_lost_title)) },
			text = {
				Text(
					text = stringResource(
						id = R.string.lbl_attempting_to_reconnect,
						stringResource(id = R.string.app_name)
					)
				)
			},
			buttons = {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(Dimensions.viewPaddingUnit),
					horizontalArrangement = Arrangement.Center,
				) {
					Button(
						onClick = {
							connectionWatcherViewModel.cancelLibraryConnectionPolling()
						},
					) {
						Text(text = stringResource(id = R.string.btn_cancel))
					}
				}
			},
			properties = DialogProperties(
				dismissOnBackPress = true,
			)
		)
	}

	val isEmptyPlaylistRequested by playlistViewModel.isClearingPlaylistRequested.subscribeAsState()
	if (isEmptyPlaylistRequested) {
		AlertDialog(
			onDismissRequest = { playlistViewModel.clearPlaylistIfGranted() },
			title = { Text(text = stringResource(id = R.string.empty_playlist)) },
			text = {
				Text(
					text = stringResource(R.string.empty_playlist_confirmation)
				)
			},
			buttons = {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(Dimensions.viewPaddingUnit),
					horizontalArrangement = Arrangement.SpaceEvenly,
				) {
					Button(
						onClick = {
							playlistViewModel.clearPlaylistIfGranted()
						},
					) {
						Text(text = stringResource(id = R.string.btn_cancel))
					}

					Button(
						onClick = {
							playlistViewModel.grantPlaylistClearing()
							playlistViewModel.clearPlaylistIfGranted()
						},
					) {
						Text(text = stringResource(id = R.string.yes))
					}
				}
			},
			properties = DialogProperties(
				dismissOnBackPress = true,
			)
		)
	}
}
