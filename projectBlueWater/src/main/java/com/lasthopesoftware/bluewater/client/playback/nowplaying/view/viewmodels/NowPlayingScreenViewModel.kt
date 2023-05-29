package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingScreenViewModel(
	private val applicationMessages: RegisterForApplicationMessages,
	private val nowPlayingDisplaySettings: StoreNowPlayingDisplaySettings,
	private val playbackService: ControlPlaybackService,
) : ViewModel(), ControlDrawerState, ControlScreenOnState
{
	private val onPlaybackStartedSubscription = applicationMessages.registerReceiver { _: PlaybackMessage.PlaybackStarted ->
		togglePlaying(true)
	}
	private val onPlaybackStoppedReceiver: (ApplicationMessage) -> Unit

	private var isPlayingState = false

	private val isDrawerShownInternalState = MutableStateFlow(false)
	private val isScreenOnEnabledState = MutableStateFlow(nowPlayingDisplaySettings.isScreenOnDuringPlayback)
	private val isScreenOnState = MutableStateFlow(false)

	val isDrawerShownState = isDrawerShownInternalState.asStateFlow()
	val isScreenOn = isScreenOnState.asStateFlow()

	override val isScreenOnEnabled = isScreenOnEnabledState.asStateFlow()
	override val isDrawerShown
		get() = isDrawerShownState.value

	init {
		onPlaybackStoppedReceiver = { togglePlaying(false) }

		with (applicationMessages) {
			registerForClass(cls<PlaybackMessage.PlaybackPaused>(), onPlaybackStoppedReceiver)
			registerForClass(cls<PlaybackMessage.PlaybackInterrupted>(), onPlaybackStoppedReceiver)
			registerForClass(cls<PlaybackMessage.PlaybackStopped>(), onPlaybackStoppedReceiver)
		}
	}

	fun initializeViewModel(libraryId: LibraryId): Promise<Unit> {
		return playbackService.promiseIsMarkedForPlay(libraryId).then(::togglePlaying)
	}

	override fun onCleared() {
		super.onCleared()

		onPlaybackStartedSubscription.close()
		applicationMessages.unregisterReceiver(onPlaybackStoppedReceiver)
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
