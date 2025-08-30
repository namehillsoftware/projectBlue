package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.SlideOutState
import com.lasthopesoftware.bluewater.android.ui.components.BackButton
import com.lasthopesoftware.bluewater.android.ui.components.MarqueeText
import com.lasthopesoftware.bluewater.android.ui.components.RatingBar
import com.lasthopesoftware.bluewater.android.ui.components.dragging.DragDropItemScope
import com.lasthopesoftware.bluewater.android.ui.components.dragging.DragDropLazyColumn
import com.lasthopesoftware.bluewater.android.ui.components.dragging.rememberDragDropListState
import com.lasthopesoftware.bluewater.android.ui.findWindow
import com.lasthopesoftware.bluewater.android.ui.linearInterpolation
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor
import com.lasthopesoftware.bluewater.android.ui.theme.SharedColors
import com.lasthopesoftware.bluewater.android.ui.theme.isStatusBarLight
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
import com.lasthopesoftware.bluewater.shared.android.UndoStack
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.toState
import com.lasthopesoftware.resources.bitmaps.ProduceBitmaps
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

val controlRowHeight = 72.dp
const val playlistControlAlpha = .8f
val minimumMenuWidth = Dimensions.listItemMenuIconSize * 3
const val disabledAlpha = .6f

class ScreenDimensionsScope(val screenHeight: Dp, val screenWidth: Dp, innerBoxScope: BoxWithConstraintsScope) :
	BoxWithConstraintsScope by innerBoxScope

@Composable
fun NowPlayingCoverArtView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
	bitmapProducer: ProduceBitmaps
) {
	Box(
		modifier = Modifier.fillMaxSize()
	) {
		val isLoadingImage by nowPlayingCoverArtViewModel.isNowPlayingImageLoading.subscribeAsState()

		val coverArt by nowPlayingCoverArtViewModel.nowPlayingImage.subscribeAsState()
		val coverArtBitmap by coverArt
			.takeIf { it.isNotEmpty() }
			?.let(bitmapProducer::promiseBitmap)
			.keepPromise()
			.toState(null, coverArt)

		coverArtBitmap
			?.let {
				Image(
					bitmap = it.asImageBitmap(),
					contentDescription = stringResource(id = R.string.img_now_playing),
					contentScale = ContentScale.Crop,
					alignment = Alignment.Center,
					modifier = Modifier.fillMaxSize(),
				)
			}

		if (isLoadingImage) {
			CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
fun NowPlayingProgressIndicator(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	modifier: Modifier = Modifier
) {
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
fun NowPlayingHeadline(
	modifier: Modifier = Modifier,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel
) {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistControls(
	modifier: Modifier = Modifier,
	playlistViewModel: NowPlayingPlaylistViewModel,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
	undoBackStack: UndoStack,
) {
	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.SpaceAround,
		verticalAlignment = Alignment.CenterVertically,
	) {
		val isEditingPlaylist by playlistViewModel.isEditingPlaylist.subscribeAsState()
		if (isEditingPlaylist) {
			Image(
				painter = painterResource(id = R.drawable.ic_remove_item_white_36dp),
				contentDescription = stringResource(id = R.string.finish_edit_now_playing_list),
				modifier = Modifier.navigable(onClick = playlistViewModel::finishPlaylistEdit),
				alpha = playlistControlAlpha,
			)
		} else {
			Image(
				painter = painterResource(id = R.drawable.pencil),
				contentDescription = stringResource(id = R.string.edit_now_playing_list),
				modifier = Modifier
					.navigable(onClick = {
						playlistViewModel.editPlaylist()
						undoBackStack.addAction { playlistViewModel.finishPlaylistEdit().toPromise() }
					}),
				alpha = playlistControlAlpha,
			)
		}

		if (isEditingPlaylist) {
			Image(
				painter = painterResource(id = R.drawable.clear_all_white_36dp),
				contentDescription = stringResource(R.string.empty_playlist),
				modifier = Modifier.navigable(onClick = playlistViewModel::requestPlaylistClearingPermission),
				alpha = playlistControlAlpha,
			)
		} else {
			val isRepeating by playlistViewModel.isRepeating.subscribeAsState()
			if (isRepeating) {
				Image(
					painter = painterResource(id = R.drawable.av_repeat_white),
					contentDescription = stringResource(id = R.string.btn_complete_playlist),
					modifier = Modifier.navigable(onClick = playlistViewModel::toggleRepeating),
					alpha = playlistControlAlpha,
				)
			} else {
				Image(
					painter = painterResource(id = R.drawable.av_no_repeat_white),
					contentDescription = stringResource(id = R.string.btn_repeat_playlist),
					modifier = Modifier.navigable(onClick = playlistViewModel::toggleRepeating),
					alpha = playlistControlAlpha,
				)
			}
		}

		if (isEditingPlaylist) {
			Image(
				painter = painterResource(id = R.drawable.upload_36dp),
				contentDescription = stringResource(id = R.string.save_playlist),
				modifier = Modifier.navigable(onClick = playlistViewModel::enableSavingPlaylist),
				alpha = playlistControlAlpha,
			)
		} else {
			val hapticFeedback = LocalHapticFeedback.current
			val isAutoScrollEnabled by playlistViewModel.isAutoScrolling.subscribeAsState()
			Image(
				painter = painterResource(id = R.drawable.baseline_playlist_play_36),
				contentDescription = stringResource(R.string.scroll_to_now_playing_item),
				modifier = Modifier.navigable(
					interactionSource = remember { MutableInteractionSource() },
					indication = ripple(),
					onClick = {
						viewModelMessageBus.sendMessage(NowPlayingMessage.ScrollToNowPlaying)
					},
					onClickLabel = stringResource(R.string.scroll_to_now_playing_item),
					onLongClick = {
						if (isAutoScrollEnabled)
							playlistViewModel.disableUserAutoScrolling()
						else
							playlistViewModel.enableUserAutoScrolling()
						hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
					},
					onLongClickLabel = stringResource(R.string.auto_scroll_to_now_playing_item)
				),
				alpha = if (isAutoScrollEnabled) 1f else disabledAlpha,
			)
		}
	}
}

@Composable
fun NowPlayingRating(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	nowPlayingScreenViewModel: NowPlayingScreenViewModel,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.height(controlRowHeight)
			.then(modifier),
		verticalArrangement = Arrangement.Center,
	) {
		val rating by nowPlayingFilePropertiesViewModel.songRating.subscribeAsState()
		val ratingInt by remember { derivedStateOf { rating.toInt() } }
		val isSongRatingEnabled by nowPlayingFilePropertiesViewModel.isSongRatingEnabled.subscribeAsState()
		val isScreenControlsVisible by nowPlayingScreenViewModel.isScreenControlsVisible.subscribeAsState()
		val isReadOnly by nowPlayingFilePropertiesViewModel.isReadOnly.subscribeAsState()
		val isRatingEnabled by remember { derivedStateOf { isSongRatingEnabled && isScreenControlsVisible && !isReadOnly } }

		var ratingColor = LocalControlColor.current
		if (!isRatingEnabled)
			ratingColor = ratingColor.copy(alpha = disabledAlpha)
		RatingBar(
			rating = ratingInt,
			color = ratingColor,
			backgroundColor = LocalControlColor.current.copy(alpha = .1f),
			modifier = Modifier
				.fillMaxWidth()
				.height(Dimensions.menuHeight),
			onRatingSelected = if (isRatingEnabled) {
				{
					nowPlayingFilePropertiesViewModel.updateRating(it.toFloat())
				}
			} else null
		)

		if (isReadOnly) {
			ProvideTextStyle(value = MaterialTheme.typography.caption) {
				Text(
					text = stringResource(id = R.string.readOnlyConnection)
				)
			}
		}
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NowPlayingPlaybackControls(
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
				.navigable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = {
						activeLibraryId?.also(playbackServiceController::previous)
					}
				),
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
				.navigable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = {
						activeLibraryId?.also(playbackServiceController::next)
					}
				)
		)
	}
}

suspend fun LazyListState.scrollToFileIfNotScrolling(file: PositionedFile) {
	if (!isScrollInProgress)
		scrollToItem(file.playlistPosition)
}

@Composable
fun NowPlayingPlaylist(
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	playlistViewModel: NowPlayingPlaylistViewModel,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
	undoBackStack: UndoStack,
	lazyListState: LazyListState,
	modifier: Modifier = Modifier,
) {
	val nowPlayingFiles by playlistViewModel.nowPlayingList.subscribeAsState()
	val activeLibraryId by nowPlayingFilePropertiesViewModel.activeLibraryId.subscribeAsState()

	val dragDropListState = rememberDragDropListState(
		lazyListState = lazyListState,
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
		var lastScrolledToItem by rememberSaveable { mutableStateOf<PositionedFile?>(null) }
		LaunchedEffect(key1 = playingFile) {
			playingFile?.also {
				if (it != lastScrolledToItem) {
					dragDropListState.lazyListState.scrollToFileIfNotScrolling(it)
					lastScrolledToItem = it
				}
			}
		}
	}

	val scope = rememberCoroutineScope()
	DisposableEffect(key1 = Unit) {
		val registration = viewModelMessageBus.registerReceiver { _: NowPlayingMessage.ScrollToNowPlaying ->
			scope.launch {
				playingFile?.apply {
					dragDropListState.lazyListState.scrollToItem(playlistPosition)
				}
			}
		}

		onDispose {
			registration.close()
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
				applicationNavigation.viewNowPlayingFileDetails(it, positionedFile)
			}
			Unit
		}

		val isEditingPlaylist by playlistViewModel.isEditingPlaylist.subscribeAsState()
		NowPlayingItemView(
			itemName = fileName,
			artist = artist,
			isActive = isPlaying,
			isEditingPlaylist = isEditingPlaylist,
			isHiddenMenuShown = isMenuShown,
			onItemClick = viewFilesClickHandler,
			onHiddenMenuClick = {
				if (!isEditingPlaylist) {
					itemListMenuBackPressedHandler.hideAllMenus()
					fileItemViewModel.showMenu()
					undoBackStack.addAction { fileItemViewModel.hideMenu().toPromise() }
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
@OptIn(ExperimentalCoroutinesApi::class)
fun BoxWithConstraintsScope.NowPlayingNarrowView(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	nowPlayingScreenViewModel: NowPlayingScreenViewModel,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
	undoBackStack: UndoStack,
	lazyListState: LazyListState,
) {
	val isScreenControlsVisible by nowPlayingScreenViewModel.isScreenControlsVisible.subscribeAsState()

	val filePropertiesHeight = remember(maxHeight) { maxHeight - expandedControlsHeight }

	val filePropertiesHeightPx = LocalDensity.current.run { filePropertiesHeight.toPx() }

	val halfScreenHeight = filePropertiesHeight / 2
	val halfScreenHeightPx = LocalDensity.current.run { halfScreenHeight.toPx() }

	var draggableState by rememberSaveable { mutableStateOf(SlideOutState.Closed) }

	val playlistDrawerState = remember {
		AnchoredDraggableState(
			initialValue = draggableState,
			anchors = DraggableAnchors {
				SlideOutState.Closed at filePropertiesHeightPx
				SlideOutState.PartiallyOpen at halfScreenHeightPx
				SlideOutState.Open at 0f
			}
		)
	}

	DisposableEffect(filePropertiesHeightPx, halfScreenHeightPx) {
		playlistDrawerState.updateAnchors(DraggableAnchors {
			SlideOutState.Closed at filePropertiesHeightPx
			SlideOutState.PartiallyOpen at halfScreenHeightPx
			SlideOutState.Open at 0f
		})

		onDispose { }
	}

	LaunchedEffect(playlistDrawerState) {
		snapshotFlow { playlistDrawerState.currentValue }.collect { draggableState = it }
	}

	val playlistEditingShownProgress by remember(playlistDrawerState) {
		derivedStateOf { playlistDrawerState.progress(SlideOutState.PartiallyOpen, SlideOutState.Open) }
	}

	val isPlaylistEditingShown by remember {
		derivedStateOf { playlistEditingShownProgress > 0 }
	}

	val playlistOpenProgress by remember(playlistDrawerState) {
		derivedStateOf { playlistDrawerState.progress(SlideOutState.Closed, SlideOutState.Open) }
	}

	val isSettledOnFirstPage by remember { derivedStateOf { playlistOpenProgress == 0f } }

	val decaySpec = LocalDensity.current.let { d ->
		remember(d) {
			SplineBasedFloatDecayAnimationSpec(d).generateDecayAnimationSpec<Float>()
		}
	}

	val velocity = LocalDensity.current.run { remember(this) { 100.dp.toPx() } }

	suspend fun hidePlaylist() {
		playlistViewModel.finishPlaylistEdit()
		playlistDrawerState.animateToWithDecay(SlideOutState.Closed, velocity, decayAnimationSpec = decaySpec)
	}

	val isPlaylistShown by playlistViewModel.isPlaylistShown.subscribeAsState()
	val scope = rememberCoroutineScope()
	DisposableEffect(isSettledOnFirstPage) {
		if (isSettledOnFirstPage) {
			playlistViewModel.hidePlaylist()
			onDispose { }
		} else {
			playlistViewModel.showPlaylist()
			val hidePlaylistAction = {
				scope.async {
					if (!playlistViewModel.isPlaylistShown.value) false
					else {
						hidePlaylist()
						true
					}
				}.toPromise()
			}

			undoBackStack.addAction(hidePlaylistAction)

			onDispose {
				undoBackStack.removeAction(hidePlaylistAction)
			}
		}
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.anchoredDraggable(
				playlistDrawerState,
				orientation = Orientation.Vertical,
			)
			.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
				onClick = { nowPlayingScreenViewModel.showControls() }
			),
	) {
		if (!isPlaylistEditingShown) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				NowPlayingHeadline(modifier = Modifier.weight(1f), nowPlayingFilePropertiesViewModel)

				if (isScreenControlsVisible) {
					val isScreenOnEnabled by nowPlayingScreenViewModel.isScreenOnEnabled.collectAsState()
					Image(
						painter = painterResource(if (isScreenOnEnabled) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp),
						alpha = .8f,
						contentDescription = stringResource(if (isScreenOnEnabled) R.string.screen_is_on else R.string.screen_is_off),
						modifier = Modifier
							.padding(Dimensions.viewPaddingUnit)
							.clickable(onClick = nowPlayingScreenViewModel::toggleScreenOn),
					)
				}
			}
		}

		Column(
			modifier = Modifier
				.offset { IntOffset(x = 0, y = playlistDrawerState.requireOffset().fastRoundToInt()) }
				.fillMaxWidth()
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.height(Dimensions.appBarHeight)
					.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceAround
			) {
				val isTopControlsShown by remember { derivedStateOf { isScreenControlsVisible || isPlaylistShown } }
				when {
					isPlaylistEditingShown -> {
						PlaylistControls(
							modifier = Modifier
								.alpha(playlistEditingShownProgress)
								.weight(1f),
							playlistViewModel = playlistViewModel,
							viewModelMessageBus = viewModelMessageBus,
							undoBackStack = undoBackStack,
						)
					}

					isTopControlsShown -> {
						BackButton(
							onBack = applicationNavigation::navigateUp,
							modifier = Modifier
								.padding(start = Dimensions.topRowOuterPadding)
								.alpha(playlistControlAlpha)
						)

						NowPlayingRating(
							nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
							nowPlayingScreenViewModel = nowPlayingScreenViewModel,
							modifier = Modifier.weight(1f)
						)
					}
				}

				if (isTopControlsShown) {
					val drawerChevronRotation by remember(playlistDrawerState) {
						derivedStateOf {
							(180 * playlistDrawerState.progress(
								SlideOutState.Closed,
								SlideOutState.PartiallyOpen
							)).coerceIn(0f, 180f)
						}
					}
					Image(
						painter = painterResource(R.drawable.chevron_up_white_36dp),
						contentDescription = stringResource(R.string.btn_hide_files),
						modifier = Modifier
							.clickable(
								onClick = {
									scope.launch {
										if (playlistDrawerState.currentValue != SlideOutState.Closed) hidePlaylist()
										else playlistDrawerState.animateToWithDecay(
											SlideOutState.PartiallyOpen,
											velocity,
											decayAnimationSpec = decaySpec
										)
									}
								},
								indication = null,
								interactionSource = remember { MutableInteractionSource() })
							.padding(end = Dimensions.topRowOuterPadding)
							.rotate(drawerChevronRotation)
							.alpha(playlistControlAlpha),
					)
				}
			}

			val progressIndicatorPadding by remember {
				derivedStateOf {
					linearInterpolation(
						Dimensions.viewPaddingUnit,
						0.dp,
						playlistOpenProgress
					)
				}
			}
			NowPlayingProgressIndicator(
				nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = progressIndicatorPadding)
			)

			val controlRowHeight by remember {
				derivedStateOf {
					linearInterpolation(
						controlRowHeight,
						0.dp,
						playlistEditingShownProgress
					)
				}
			}
			if (!isPlaylistEditingShown) {
				NowPlayingPlaybackControls(
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					playbackServiceController = playbackServiceController,
				)
			} else {
				Spacer(modifier = Modifier.height(controlRowHeight))
			}

			if (isPlaylistShown) {
				val playlistDrawerOffset by LocalDensity.current.run {
					remember(LocalDensity.current, playlistDrawerState) {
						derivedStateOf {
							playlistDrawerState.requireOffset().toDp()
						}
					}
				}

				val playlistHeight by LocalDensity.current.run {
					remember(LocalDensity.current, playlistDrawerState, this@NowPlayingNarrowView.maxHeight) {
						derivedStateOf {
							this@NowPlayingNarrowView.maxHeight - playlistDrawerOffset - controlRowHeight - progressIndicatorPadding - ProgressIndicatorDefaults.StrokeWidth - Dimensions.appBarHeight
						}
					}
				}

				NowPlayingPlaylist(
					childItemViewModelProvider,
					nowPlayingFilePropertiesViewModel,
					applicationNavigation,
					playbackServiceController,
					itemListMenuBackPressedHandler,
					playlistViewModel,
					viewModelMessageBus,
					undoBackStack,
					lazyListState,
					modifier = Modifier
						.height(playlistHeight)
						.background(SharedColors.overlayDark)
				)
			}
		}

		DisposableEffect(key1 = isPlaylistEditingShown) {
			if (!isPlaylistEditingShown) {
				playlistViewModel.enableSystemAutoScrolling()
			} else {
				playlistViewModel.disableSystemAutoScrolling()
			}

			onDispose {
				playlistViewModel.disableSystemAutoScrolling()
			}
		}
	}
}

@ExperimentalFoundationApi
@Composable
private fun ScreenDimensionsScope.NowPlayingWideView(
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	playbackServiceController: ControlPlaybackService,
	nowPlayingScreenViewModel: NowPlayingScreenViewModel,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
	undoBackStack: UndoStack,
	lazyListState: LazyListState
) {
	val playlistWidth = screenHeight.coerceIn(minimumMenuWidth, maxWidth / 2)
	val playlistWidthPx = LocalDensity.current.run { playlistWidth.toPx() }

	var draggableState by rememberSaveable { mutableStateOf(SlideOutState.Open) }

	val playlistDrawerState = remember {
		AnchoredDraggableState(
			initialValue = draggableState,
			anchors = DraggableAnchors {
				SlideOutState.Closed at 0f
				SlideOutState.Open at playlistWidthPx
			}
		)
	}

	DisposableEffect(playlistWidthPx) {
		playlistDrawerState.updateAnchors(DraggableAnchors {
			SlideOutState.Closed at 0f
			SlideOutState.Open at playlistWidthPx
		})

		onDispose { }
	}

	LaunchedEffect(playlistDrawerState) {
		snapshotFlow { playlistDrawerState.currentValue }.collect { draggableState = it }
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.anchoredDraggable(playlistDrawerState, true, Orientation.Horizontal),
	) {
		val nowPlayingPaneWidth = LocalDensity.current.run {
			this@NowPlayingWideView.maxWidth - playlistDrawerState.requireOffset().toDp()
		}

		val playlistOpenProgress by remember(playlistDrawerState) {
			derivedStateOf {
				playlistDrawerState.progress(
					SlideOutState.Closed,
					SlideOutState.Open
				)
			}
		}

		Box(
			modifier = Modifier
				.fillMaxHeight()
				.width(nowPlayingPaneWidth)
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = { nowPlayingScreenViewModel.showControls() }
				)
		) {
			val isScreenControlsVisible by nowPlayingScreenViewModel.isScreenControlsVisible.subscribeAsState()
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				NowPlayingHeadline(
					modifier = Modifier.weight(1f),
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel
				)

				if (isScreenControlsVisible) {
					val isScreenOnEnabled by nowPlayingScreenViewModel.isScreenOnEnabled.collectAsState()
					Image(
						painter = painterResource(if (isScreenOnEnabled) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp),
						alpha = .8f,
						contentDescription = stringResource(if (isScreenOnEnabled) R.string.screen_is_on else R.string.screen_is_off),
						modifier = Modifier
							.padding(Dimensions.viewPaddingUnit)
							.clickable(onClick = nowPlayingScreenViewModel::toggleScreenOn),
					)
				}
			}

			Column(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
			) {
				if (isScreenControlsVisible) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier
							.height(Dimensions.appBarHeight)
							.padding(start = Dimensions.topRowOuterPadding, end = Dimensions.topRowOuterPadding)
							.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						BackButton(onBack = applicationNavigation::navigateUp)

						NowPlayingRating(
							nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
							nowPlayingScreenViewModel = nowPlayingScreenViewModel,
							modifier = Modifier.weight(1f),
						)

						val drawerChevronRotation by remember {
							derivedStateOf { -90 + (180 * playlistOpenProgress).coerceIn(0f, 180f) }
						}
						val scope = rememberCoroutineScope()
						Image(
							painter = painterResource(R.drawable.chevron_up_white_36dp),
							contentDescription = stringResource(R.string.btn_hide_files),
							modifier = Modifier
								.clickable(
									onClick = {
										scope.launch {
											if (playlistDrawerState.currentValue == SlideOutState.Open) {
												playlistViewModel.finishPlaylistEdit()
												scope.launch {
													playlistDrawerState.animateTo(SlideOutState.Closed)
												}
											} else {
												playlistDrawerState.animateTo(SlideOutState.Open)
											}
										}
									},
									indication = null,
									interactionSource = remember { MutableInteractionSource() }
								)
								.rotate(drawerChevronRotation),
						)
					}
				}

				NowPlayingProgressIndicator(
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					modifier = Modifier
						.fillMaxWidth()
						.padding(
							top = Dimensions.viewPaddingUnit,
							bottom = Dimensions.viewPaddingUnit
						)
				)

				NowPlayingPlaybackControls(
					nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
					playbackServiceController = playbackServiceController,
				)
			}
		}

		if (playlistOpenProgress > 0f) {
			Column(
				modifier = Modifier
					.fillMaxHeight()
					.width(playlistWidth)
					.offset { IntOffset(x = nowPlayingPaneWidth.roundToPx(), y = 0) }
					.background(SharedColors.overlayDark),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				DisposableEffect(key1 = playlistDrawerState.currentValue) {
					if (playlistDrawerState.currentValue == SlideOutState.Closed) {
						playlistViewModel.enableSystemAutoScrolling()
					} else {
						playlistViewModel.disableSystemAutoScrolling()
					}

					onDispose {
						playlistViewModel.disableSystemAutoScrolling()
					}
				}

				PlaylistControls(
					modifier = Modifier
						.fillMaxWidth()
						.height(Dimensions.appBarHeight),
					playlistViewModel = playlistViewModel,
					viewModelMessageBus = viewModelMessageBus,
					undoBackStack = undoBackStack,
				)

				NowPlayingPlaylist(
					childItemViewModelProvider,
					nowPlayingFilePropertiesViewModel,
					applicationNavigation,
					playbackServiceController,
					itemListMenuBackPressedHandler,
					playlistViewModel,
					viewModelMessageBus = viewModelMessageBus,
					undoBackStack = undoBackStack,
					lazyListState = lazyListState,
					modifier = Modifier
						.fillMaxHeight()
						.onFocusChanged { f ->
							if (f.hasFocus) playlistViewModel.lockOutAutoScroll()
							else playlistViewModel.releaseAutoScroll()
						},
				)
			}
		}
	}
}

@ExperimentalFoundationApi
@Composable
fun NowPlayingView(
	nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel,
	nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
	nowPlayingScreenViewModel: NowPlayingScreenViewModel,
	playbackServiceController: ControlPlaybackService,
	playlistViewModel: NowPlayingPlaylistViewModel,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	connectionWatcherViewModel: ConnectionWatcherViewModel,
	viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
	bitmapProducer: ProduceBitmaps,
	undoBackStack: UndoStack,
) {
	val isScreenOn by nowPlayingScreenViewModel.isScreenOn.collectAsState()
	KeepScreenOn(isScreenOn)

	findWindow()?.isStatusBarLight = false

	ControlSurface(
		color = Color.Transparent,
		contentColor = Color.White,
		controlColor = Color.White,
	) {
		NowPlayingCoverArtView(
			nowPlayingCoverArtViewModel = nowPlayingCoverArtViewModel,
			bitmapProducer = bitmapProducer
		)

		BoxWithConstraints(
			modifier = Modifier
				.fillMaxSize()
				.background(SharedColors.overlayDark)
		) screenBox@{
			Column {
				Spacer(
					modifier = Modifier
						.windowInsetsTopHeight(WindowInsets.systemBars)
						.fillMaxWidth()
						.background(SharedColors.overlayDark)
				)

				Row(modifier = Modifier.weight(1f)) {
					Spacer(
						modifier = Modifier
							.windowInsetsStartWidth(WindowInsets.systemBars)
							.fillMaxHeight()
							.background(SharedColors.overlayDark)
					)

					// Nest boxes to get proper size constraints
					BoxWithConstraints(modifier = Modifier.weight(1f)) nestedBox@{
						val screenScope = ScreenDimensionsScope(
							screenHeight = this@screenBox.maxHeight,
							screenWidth = this@screenBox.maxWidth,
							innerBoxScope = this@nestedBox
						)

						with(screenScope) {
							val playingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.subscribeAsState()
							val lazyListState = rememberLazyListState(playingFile?.playlistPosition ?: 0)

							if (screenWidth < Dimensions.twoColumnThreshold) {
								NowPlayingNarrowView(
									nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
									nowPlayingScreenViewModel = nowPlayingScreenViewModel,
									playbackServiceController = playbackServiceController,
									playlistViewModel = playlistViewModel,
									childItemViewModelProvider = childItemViewModelProvider,
									applicationNavigation = applicationNavigation,
									itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
									viewModelMessageBus = viewModelMessageBus,
									undoBackStack = undoBackStack,
									lazyListState = lazyListState,
								)
							} else {
								NowPlayingWideView(
									nowPlayingFilePropertiesViewModel = nowPlayingFilePropertiesViewModel,
									nowPlayingScreenViewModel = nowPlayingScreenViewModel,
									playbackServiceController = playbackServiceController,
									playlistViewModel = playlistViewModel,
									childItemViewModelProvider = childItemViewModelProvider,
									applicationNavigation = applicationNavigation,
									itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
									viewModelMessageBus = viewModelMessageBus,
									undoBackStack = undoBackStack,
									lazyListState = lazyListState,
								)
							}
						}
					}

					Spacer(
						modifier = Modifier
							.windowInsetsEndWidth(WindowInsets.systemBars)
							.fillMaxHeight()
							.background(SharedColors.overlayDark)
					)
				}

				Spacer(
					modifier = Modifier
						.windowInsetsBottomHeight(WindowInsets.systemBars)
						.fillMaxWidth()
						.background(SharedColors.overlayDark)
				)
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

	val isConnectionLost by connectionWatcherViewModel.isCheckingConnection.subscribeAsState()
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
