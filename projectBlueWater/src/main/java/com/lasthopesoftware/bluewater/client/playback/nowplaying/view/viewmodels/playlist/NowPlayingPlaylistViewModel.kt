package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.ui.components.dragging.move
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingPlaylistViewModel(
	applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState
) :
	ViewModel(),
	ControlPlaylistEdits,
	HasEditPlaylistState,
	(LibraryPlaybackMessage.PlaylistChanged) -> Unit
{
	private var activeLibraryId: LibraryId? = null
	private val playlistChangedSubscription = applicationMessages.registerReceiver(this)

	private val mutableEditingPlaylistState = MutableStateFlow(false)
	private val nowPlayingListState = MutableStateFlow(emptyList<PositionedFile>())

	init {
		updateViewFromRepository()
	}

	val isEditingPlaylistState = mutableEditingPlaylistState.asStateFlow()
	val nowPlayingList = nowPlayingListState.asStateFlow()

	fun initializeView(libraryId: LibraryId) {
		activeLibraryId = libraryId
	}

	override val isEditingPlaylist: Boolean
		get() = isEditingPlaylistState.value

	override fun editPlaylist() {
		mutableEditingPlaylistState.value = true
	}

	override fun finishPlaylistEdit() {
		mutableEditingPlaylistState.value = false
	}

	fun swapFiles(from: Int, to: Int) {
		nowPlayingListState.value = nowPlayingListState.value.toMutableList().move(from, to)
	}

	override fun invoke(p1: LibraryPlaybackMessage.PlaylistChanged) {
		updateViewFromRepository()
	}

	override fun onCleared() {
		playlistChangedSubscription.close()
	}

	private fun updateViewFromRepository() {
		activeLibraryId
			?.let(nowPlayingRepository::promiseNowPlaying)
			.keepPromise()
			.then { np ->
				nowPlayingListState.value = np?.playlist?.mapIndexed(::PositionedFile) ?: emptyList()
			}
	}
}
