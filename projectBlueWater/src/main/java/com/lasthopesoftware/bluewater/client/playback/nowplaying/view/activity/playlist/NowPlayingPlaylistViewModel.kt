package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.playlist

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage.PlaylistChanged
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingPlaylistViewModel(
	applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState
) :
	ViewModel(),
	ControlPlaylistEdits,
	HasEditPlaylistState,
	(PlaylistChanged) -> Unit
{
	private val playlistChangedSubscription = applicationMessages.registerReceiver(this)

	private val mutableEditingPlaylistState = MutableStateFlow(false)
	private val nowPlayingListState = MutableStateFlow(emptyList<PositionedFile>())

	init {
		updateViewFromRepository()
	}

	val isEditingPlaylistState = mutableEditingPlaylistState.asStateFlow()
	val nowPlayingList = nowPlayingListState.asStateFlow()

	override val isEditingPlaylist: Boolean
		get() = isEditingPlaylistState.value

	override fun editPlaylist() {
		mutableEditingPlaylistState.value = true
	}

	override fun finishPlaylistEdit() {
		mutableEditingPlaylistState.value = false
	}

	fun swapFiles(from: Int, to: Int) {
		nowPlayingListState.value = nowPlayingListState.value.toMutableList().apply {
			add(to, removeAt(from))
		}
	}

	override fun invoke(p1: PlaylistChanged) {
		updateViewFromRepository()
	}

	override fun onCleared() {
		playlistChangedSubscription.close()
	}

	private fun updateViewFromRepository() {
		nowPlayingRepository
			.promiseNowPlaying()
			.then { np ->
				nowPlayingListState.value = np?.playlist?.mapIndexed(::PositionedFile) ?: emptyList()
			}
	}
}
