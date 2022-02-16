package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels

import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.messages.RegisterForMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingScreenViewModel(
	private val messages: RegisterForMessages,
	private val nowPlayingDisplaySettings: StoreNowPlayingDisplaySettings,
	playbackService: ControlPlaybackService,
) : ViewModel(), ControlDrawerState, ControlScreenOnState
{
	private val onPlaybackStartedReceiver: ReceiveBroadcastEvents
	private val onPlaybackStoppedReceiver: ReceiveBroadcastEvents

	private var isPlayingState = false

	private val isDrawerShownInternalState = MutableStateFlow(false)
	private val isScreenOnEnabledState = MutableStateFlow(nowPlayingDisplaySettings.isScreenOnDuringPlayback)
	private val isScreenOnState = MutableStateFlow(false)

	val isDrawerShownState = isDrawerShownInternalState.asStateFlow()
	override val isDrawerShown = isDrawerShownState.value
	override val isScreenOnEnabled = isScreenOnEnabledState.asStateFlow()
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

		playbackService.promiseIsMarkedForPlay().then(::togglePlaying)
	}

	override fun onCleared() {
		super.onCleared()

		with(messages) {
			unregisterReceiver(onPlaybackStoppedReceiver)
			unregisterReceiver(onPlaybackStartedReceiver)
		}
	}

	override fun showDrawer() {
		isDrawerShownInternalState.value = true
	}

	override fun hideDrawer() {
		isDrawerShownInternalState.value = false
	}

	override fun toggleScreenOn() {
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
