package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels

import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingViewModel(
	private val messages: RegisterForMessages,
	private val nowPlayingDisplaySettings: StoreNowPlayingDisplaySettings
) : ViewModel(), ControlDrawerState
{
	private val onPlaybackStartedReceiver: ReceiveBroadcastEvents
	private val onPlaybackStoppedReceiver: ReceiveBroadcastEvents

	private var isPlayingState = false

	private val isDrawerShownState = MutableStateFlow(false)
	private val isScreenOnEnabledState = MutableStateFlow(false)
	private val isScreenOnState = MutableStateFlow(false)

	val isDrawerShown = isDrawerShownState.asStateFlow()
	val isScreenOnEnabled = isScreenOnEnabledState.asStateFlow()
	val isScreenOn = isScreenOnState.asStateFlow()

	init {
		onPlaybackStartedReceiver = ReceiveBroadcastEvents { togglePlaying(true) }
		onPlaybackStoppedReceiver = ReceiveBroadcastEvents { togglePlaying(false) }

		val playbackStoppedIntentFilter = IntentFilter().apply {
			addAction(PlaylistEvents.onPlaylistPause)
			addAction(PlaylistEvents.onPlaylistInterrupted)
			addAction(PlaylistEvents.onPlaylistStop)
		}

		with(messages) {
			registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter)
			registerReceiver(onPlaybackStartedReceiver, IntentFilter(PlaylistEvents.onPlaylistStart))
		}
	}

	override fun onCleared() {
		super.onCleared()

		with(messages) {
			unregisterReceiver(onPlaybackStoppedReceiver)
			unregisterReceiver(onPlaybackStartedReceiver)
		}
	}

	override fun showDrawer() {
		isDrawerShownState.value = true
	}

	override fun hideDrawer() {
		isDrawerShownState.value = false
	}

	fun toggleScreenOn() {
		isScreenOnEnabledState.value = !isScreenOnEnabledState.value
		nowPlayingDisplaySettings.isScreenOnDuringPlayback = isScreenOnEnabledState.value
		updateKeepScreenOnStatus()
	}

	private fun togglePlaying(isPlaying: Boolean) {
		isPlayingState = isPlaying
		updateKeepScreenOnStatus()
	}

	private fun updateKeepScreenOnStatus() {
		isScreenOnState.value = isPlayingState && isScreenOnEnabledState.value
	}
}
