package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.observables.LiftedInteractionState
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.promises.PromiseDelay
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingScreenViewModel(
	private val applicationMessages: RegisterForApplicationMessages,
	nowPlayingMessages: RegisterForTypedMessages<NowPlayingMessage>,
	private val nowPlayingDisplaySettings: StoreNowPlayingDisplaySettings,
	private val playbackService: ControlPlaybackService,
) : ViewModel(), ControlDrawerState, ControlScreenOnState
{
	private val onPlaybackStartedSubscription = applicationMessages.registerReceiver { _: PlaybackMessage.PlaybackStarted ->
		togglePlaying(true)
	}
	private val onPlaybackStoppedReceiver: (ApplicationMessage) -> Unit = { togglePlaying(false) }
	private val onTrackChangedSubscription = applicationMessages.registerReceiver { m: LibraryPlaybackMessage.TrackChanged ->
		if (m.libraryId == activeLibraryId) {
			showControls()
		}
	}
	private val filePropertiesShownSubscription = nowPlayingMessages.registerReceiver { _: NowPlayingMessage.FilePropertiesLoaded ->
		showControls()
	}

	private var controlsShownPromise = Promise.empty<Any?>()
	private var isPlayingState = false
	private var activeLibraryId: LibraryId? = null

	private val isDrawerShownInternalState = MutableStateFlow(false)
	private val isScreenOnEnabledState = MutableStateFlow(nowPlayingDisplaySettings.isScreenOnDuringPlayback)
	private val isScreenOnState = MutableStateFlow(false)
	private val isScreenControlsVisibleState = MutableInteractionState(false)
	private val isScreenControlsAlwaysVisibleState = MutableInteractionState(false)
	val isScreenControlsVisible = LiftedInteractionState(
		Observable.combineLatest(
			isScreenControlsVisibleState,
			isScreenControlsAlwaysVisibleState
		) { screen, always -> screen.value || always.value },
		false
	)

	val isDrawerShownState = isDrawerShownInternalState.asStateFlow()
	val isScreenOn = isScreenOnState.asStateFlow()

	override val isScreenOnEnabled = isScreenOnEnabledState.asStateFlow()
	override val isDrawerShown
		get() = isDrawerShownState.value

	init {
		with (applicationMessages) {
			registerForClass(cls<PlaybackMessage.PlaybackPaused>(), onPlaybackStoppedReceiver)
			registerForClass(cls<PlaybackMessage.PlaybackInterrupted>(), onPlaybackStoppedReceiver)
			registerForClass(cls<PlaybackMessage.PlaybackStopped>(), onPlaybackStoppedReceiver)
		}
	}

	fun initializeViewModel(libraryId: LibraryId): Promise<Unit> {
		activeLibraryId = libraryId
		return playbackService.promiseIsMarkedForPlay(libraryId).then(::togglePlaying)
	}

	@Synchronized
	fun showControls() {
		controlsShownPromise.cancel()

		isScreenControlsVisibleState.value = true
		controlsShownPromise = Promise.Proxy { cp ->
			PromiseDelay
				.delay<Any?>(nowPlayingDisplaySettings.screenControlVisibilityTime)
				.also(cp::doCancel)
				.then(
					{ _, cs ->
						if (!cs.isCancelled)
							isScreenControlsVisibleState.value = false
					},
					{ _, _ ->
						// ignored - handle to avoid excessive logging
					}
				)
		}
	}

	fun alwaysShowControls() {
		isScreenControlsAlwaysVisibleState.value = true
	}

	fun disableAlwaysShowingControls() {
		isScreenControlsAlwaysVisibleState.value = false
	}

	override fun onCleared() {
		super.onCleared()

		onPlaybackStartedSubscription.close()
		filePropertiesShownSubscription.close()
		onTrackChangedSubscription.close()
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
