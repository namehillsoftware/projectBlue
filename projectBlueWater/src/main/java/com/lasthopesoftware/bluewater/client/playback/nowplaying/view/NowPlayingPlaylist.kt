package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.android.ui.components.dragging.DragDropItemScope
import com.lasthopesoftware.bluewater.android.ui.components.dragging.DragDropLazyColumn
import com.lasthopesoftware.bluewater.android.ui.components.dragging.rememberDragDropListState
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
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

		val isMenuShown by fileItemViewModel.isMenuShown.subscribeAsState()
		val fileName by fileItemViewModel.title.subscribeAsState()
		val artist by fileItemViewModel.artist.subscribeAsState()
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
