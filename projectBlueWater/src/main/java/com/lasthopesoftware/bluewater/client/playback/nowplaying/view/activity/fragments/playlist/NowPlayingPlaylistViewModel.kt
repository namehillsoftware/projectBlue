package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistChanged
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingPlaylistViewModel(
	private val applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val typedMessageBus: SendTypedMessages<NowPlayingPlaylistMessage>
) :
	ViewModel(),
	ControlPlaylistEdits,
	HasEditPlaylistState,
	(PlaylistChanged) -> Unit
{
	private val mutableEditingPlaylistState = MutableStateFlow(false)
	private val nowPlayingListState = MutableStateFlow(emptyList<PositionedFile>())

	init {
		applicationMessages.registerReceiver(this)
		updateViewFromRepository()
	}

	val isEditingPlaylistState = mutableEditingPlaylistState.asStateFlow()
	val nowPlayingList = nowPlayingListState.asStateFlow()

	override val isEditingPlaylist: Boolean
		get() = isEditingPlaylistState.value

	override fun editPlaylist() {
		mutableEditingPlaylistState.value = true
		typedMessageBus.sendMessage(EditPlaylist)
	}

	override fun finishPlaylistEdit() {
		mutableEditingPlaylistState.value = false
		typedMessageBus.sendMessage(FinishEditPlaylist)
	}

	override fun invoke(p1: PlaylistChanged) {
		updateViewFromRepository()
	}

	override fun onCleared() {
		applicationMessages.unregisterReceiver(this)
	}

	private fun updateViewFromRepository() {
		nowPlayingRepository
			.promiseNowPlaying()
			.then { np ->
				nowPlayingListState.value = np?.playlist?.mapIndexed(::PositionedFile) ?: emptyList()
			}
	}
}
