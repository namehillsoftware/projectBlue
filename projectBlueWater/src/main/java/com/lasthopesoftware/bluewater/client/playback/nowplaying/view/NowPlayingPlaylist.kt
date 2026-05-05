package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.components.dragging.DragDropItemScope
import com.lasthopesoftware.bluewater.android.ui.components.dragging.DragDropLazyColumn
import com.lasthopesoftware.bluewater.android.ui.components.dragging.rememberDragDropListState
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.NowPlayingItemView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.UndoStack
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.toPromise
import kotlinx.coroutines.launch

@Composable
fun DragDropItemScope.NowPlayingFileView(
	activeLibraryId: LibraryId?,
	positionedFile: PositionedFile,
	isPlaying: Boolean,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	playlistViewModel: NowPlayingPlaylistViewModel,
	undoBackStack: UndoStack,
) {
	val fileItemViewModel = remember(childItemViewModelProvider::getViewModel)

	DisposableEffect(activeLibraryId, positionedFile) {
		activeLibraryId?.also {
			fileItemViewModel.promiseUpdate(it, positionedFile.serviceFile)
		}

		onDispose {
			fileItemViewModel.reset()
		}
	}

	val isMenuShown by fileItemViewModel.isMenuShown.subscribeAsState()
	val fileName by fileItemViewModel.title.subscribeAsState()
	val artist by fileItemViewModel.artist.subscribeAsState()

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

@Composable
fun NowPlayingFileView(
	activeLibraryId: LibraryId?,
	positionedFile: PositionedFile,
	isPlaying: Boolean,
	childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
	applicationNavigation: NavigateApplication,
	playbackServiceController: ControlPlaybackService,
	itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler,
	playlistViewModel: NowPlayingPlaylistViewModel,
	undoBackStack: UndoStack,
) {
	val fileItemViewModel = remember(childItemViewModelProvider::getViewModel)

	DisposableEffect(activeLibraryId, positionedFile) {
		activeLibraryId?.also {
			fileItemViewModel.promiseUpdate(it, positionedFile.serviceFile)
		}

		onDispose {
			fileItemViewModel.reset()
		}
	}

	val isMenuShown by fileItemViewModel.isMenuShown.subscribeAsState()
	val fileName by fileItemViewModel.title.subscribeAsState()
	val artist by fileItemViewModel.artist.subscribeAsState()

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
		},
		onMoveItemUp = {
			activeLibraryId?.let {
				val currentPosition = positionedFile.playlistPosition
				val newPosition = currentPosition - 1
				playlistViewModel.swapFiles(currentPosition, newPosition)
				playbackServiceController.moveFile(it, currentPosition, newPosition)
			}
		},
		onMoveItemDown = {
			activeLibraryId?.let {
				val currentPosition = positionedFile.playlistPosition
				val newPosition = currentPosition + 1
				playlistViewModel.swapFiles(currentPosition, newPosition)
				playbackServiceController.moveFile(it, currentPosition, newPosition)
			}
		},
	)
}

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
				painter = painterResource(id = R.drawable.cancel_36dp),
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
		val registration =
			viewModelMessageBus.registerReceiver { _: NowPlayingMessage.ScrollToNowPlaying ->
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

	val inputMode = LocalInputModeManager.current
	if (inputMode.inputMode == InputMode.Touch) {
		DragDropLazyColumn(
			dragDropListState = dragDropListState,
			modifier = modifier,
		) {
			dragDropItems(items = nowPlayingFiles, keyFactory = { _, f -> f }) { _, f ->
				NowPlayingFileView(
					activeLibraryId = activeLibraryId,
					positionedFile = f,
					isPlaying = playingFile == f,
					childItemViewModelProvider = childItemViewModelProvider,
					applicationNavigation = applicationNavigation,
					playbackServiceController = playbackServiceController,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					playlistViewModel = playlistViewModel,
					undoBackStack = undoBackStack,
				)
			}
		}
	} else {
		LazyColumn(modifier = modifier) {
			items(items = nowPlayingFiles, key = { it }) { f ->
				NowPlayingFileView(
					activeLibraryId = activeLibraryId,
					positionedFile = f,
					isPlaying = playingFile == f,
					childItemViewModelProvider = childItemViewModelProvider,
					applicationNavigation = applicationNavigation,
					playbackServiceController = playbackServiceController,
					itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					playlistViewModel = playlistViewModel,
					undoBackStack = undoBackStack,
				)
			}
		}
	}
}
