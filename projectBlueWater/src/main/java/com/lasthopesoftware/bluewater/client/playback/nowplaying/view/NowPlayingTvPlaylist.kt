package com.lasthopesoftware.bluewater.client.playback.nowplaying.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.list.ViewPlaylistFileItem
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.components.NowPlayingItemView
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.observables.subscribeAsState
import kotlinx.coroutines.launch

@Composable
fun NowPlayingTvPlaylist(
    childItemViewModelProvider: PooledCloseablesViewModel<ViewPlaylistFileItem>,
    nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel,
    applicationNavigation: NavigateApplication,
    playbackServiceController: ControlPlaybackService,
    playlistViewModel: NowPlayingPlaylistViewModel,
    viewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>,
    modifier: Modifier = Modifier,
) {
	val nowPlayingFiles by playlistViewModel.nowPlayingList.subscribeAsState()
	val activeLibraryId by nowPlayingFilePropertiesViewModel.activeLibraryId.subscribeAsState()

	val lazyListState = rememberLazyListState()

	val playingFile by nowPlayingFilePropertiesViewModel.nowPlayingFile.subscribeAsState()

	val isAutoScrollEnabled by playlistViewModel.isAutoScrolling.subscribeAsState()
	if (isAutoScrollEnabled) {
        LaunchedEffect(key1 = playingFile) {
            playingFile?.apply {
                if (!lazyListState.isScrollInProgress)
                    lazyListState.scrollToItem(playlistPosition)
            }
        }
	}

	val scope = rememberCoroutineScope()
    DisposableEffect(key1 = Unit) {
        val registration =
            viewModelMessageBus.registerReceiver { _: NowPlayingMessage.ScrollToNowPlaying ->
                scope.launch {
                    playingFile?.apply {
                        lazyListState.scrollToItem(playlistPosition)
                    }
                }
            }

        onDispose {
            registration.close()
        }
    }

	val isEditing by playlistViewModel.isEditingPlaylist.subscribeAsState()

	@Composable
	fun NowPlayingFileView(positionedFile: PositionedFile) {
		val fileItemViewModel = remember(childItemViewModelProvider::getViewModel)

		DisposableEffect(activeLibraryId, positionedFile) {
			activeLibraryId?.also {
				fileItemViewModel.promiseUpdate(it, positionedFile.serviceFile)
			}

			onDispose {
				fileItemViewModel.reset()
			}
		}

		val fileName by fileItemViewModel.title.subscribeAsState()
		val artist by fileItemViewModel.artist.subscribeAsState()
		val isPlaying by remember { derivedStateOf { playingFile == positionedFile } }

        NowPlayingItemView(
            itemName = fileName,
            artist = artist,
            isActive = isPlaying,
            isEditingPlaylist = isEditing,
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
            onItemClick = {
                activeLibraryId?.also {
                    applicationNavigation.viewNowPlayingFileDetails(it, positionedFile)
                }
            },
            onRemoveFromNowPlayingClick = {
                activeLibraryId?.also {
                    playbackServiceController
                        .removeFromPlaylistAtPosition(it, positionedFile.playlistPosition)
                }
            }
        )
	}

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
			.focusGroup()
			.onFocusChanged { f ->
				if (f.hasFocus) playlistViewModel.lockOutAutoScroll()
				else playlistViewModel.releaseAutoScroll()
			}
			.then(modifier),
    ) {
        items(items = nowPlayingFiles, key = { f -> f }) { f ->
            NowPlayingFileView(positionedFile = f)
        }
    }
}
