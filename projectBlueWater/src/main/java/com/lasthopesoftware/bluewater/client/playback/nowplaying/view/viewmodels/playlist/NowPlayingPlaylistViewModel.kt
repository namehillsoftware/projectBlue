package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.StorePlaylists
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.move
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingPlaylistViewModel(
	applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val playbackService: ControlPlaybackService,
	private val playlistStorage: StorePlaylists,
) :
	ViewModel(),
	ControlPlaylistEdits,
	HasEditPlaylistState,
	(LibraryPlaybackMessage.PlaylistChanged) -> Unit
{
	private var activeLibraryId: LibraryId? = null
	private val playlistChangedSubscription = applicationMessages.registerReceiver(this)

	private val isRepeatingState = MutableStateFlow(false)
	private val mutableEditingPlaylistState = MutableStateFlow(false)
	private val nowPlayingListState = MutableStateFlow(emptyList<PositionedFile>())
	private val playlistPathsState = MutableStateFlow(emptyList<String>())

	val isRepeating = isRepeatingState.asStateFlow()
	val isEditingPlaylistState = mutableEditingPlaylistState.asStateFlow()
	val nowPlayingList = nowPlayingListState.asStateFlow()
	val playlistPaths = playlistPathsState.asStateFlow()

	fun initializeView(libraryId: LibraryId): Promise<Unit> {
		activeLibraryId = libraryId
		return updateViewFromRepository()
	}

	override val isEditingPlaylist: Boolean
		get() = isEditingPlaylistState.value

	override fun editPlaylist() {
		mutableEditingPlaylistState.value = true
	}

	override fun finishPlaylistEdit() {
		mutableEditingPlaylistState.value = false
	}

	fun savePlaylist(path: String): Promise<*> = activeLibraryId?.let { libraryId ->
		playlistStorage.promiseStoredPlaylist(libraryId, path, nowPlayingList.value.map { it.serviceFile })
	}.keepPromise()

	fun swapFiles(from: Int, to: Int) {
		nowPlayingListState.value = nowPlayingListState.value.toMutableList().move(from, to)
	}

	fun toggleRepeating() {
		with (isRepeatingState) {
			value = !value

			activeLibraryId?.also {
				if (value) playbackService.setRepeating(it)
				else playbackService.setCompleting(it)
			}
		}
	}

	override fun invoke(p1: LibraryPlaybackMessage.PlaylistChanged) {
		if (p1.libraryId == activeLibraryId)
			updateViewFromRepository()
	}

	override fun onCleared() {
		playlistChangedSubscription.close()
	}

	private fun updateViewFromRepository() =
		activeLibraryId
			?.let(nowPlayingRepository::promiseNowPlaying)
			.keepPromise()
			.then { np ->
				nowPlayingListState.value = np?.positionedPlaylist ?: emptyList()
				isRepeatingState.value = np?.isRepeating ?: false
			}
}
