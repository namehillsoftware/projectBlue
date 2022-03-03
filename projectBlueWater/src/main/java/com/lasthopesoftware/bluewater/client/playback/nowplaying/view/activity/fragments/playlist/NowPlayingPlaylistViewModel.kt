package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingPlaylistViewModel(
	private val messages: RegisterForMessages,
	private val nowPlayingRepository: GetNowPlayingState,
	private val typedMessageBus: SendTypedMessages<NowPlayingPlaylistMessage>
) :
	ViewModel(),
	ControlPlaylistEdits,
	HasEditPlaylistState
{
	private val onPlaylistChangedReceiver: ReceiveBroadcastEvents

	private val mutableEditingPlaylistState = MutableStateFlow(false)
	private val nowPlayingListState = MutableStateFlow(emptyList<PositionedFile>())

	init {
		onPlaylistChangedReceiver = ReceiveBroadcastEvents { updateViewFromRepository() }
		messages.registerReceiver(onPlaylistChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistChange))
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

	override fun onCleared() {
		messages.unregisterReceiver(onPlaylistChangedReceiver)
	}

	private fun updateViewFromRepository() {
		nowPlayingRepository
			.promiseNowPlaying()
			.then { np ->
				nowPlayingListState.value = np?.playlist?.mapIndexed(::PositionedFile) ?: emptyList()
			}
	}
}
