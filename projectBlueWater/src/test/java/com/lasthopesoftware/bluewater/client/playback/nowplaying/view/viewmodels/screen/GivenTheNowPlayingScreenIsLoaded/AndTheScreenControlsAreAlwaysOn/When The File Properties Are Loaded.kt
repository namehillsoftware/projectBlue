package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.screen.GivenTheNowPlayingScreenIsLoaded.AndTheScreenControlsAreAlwaysOn

import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.PromiseDelay
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When The File Properties Are Loaded` {
	private val mut by lazy {
		val nowPlayingMessageBus = RecordingTypedMessageBus<NowPlayingMessage>()
		val vm = NowPlayingScreenViewModel(
			RecordingApplicationMessageBus(),
			nowPlayingMessageBus,
			InMemoryNowPlayingDisplaySettings(),
			mockk(),
		)

		Pair(nowPlayingMessageBus, vm)
	}

	private var controlsShownAfterTimeout = false
	private var controlsShownAfterDisablingAlwaysShown = false
	private var controlsShownAfterEnablingAlwaysShown = false
	private var testStartTime = 0L

	@BeforeAll
	fun act() {
		testStartTime = System.currentTimeMillis()
		val (bus, vm) = mut
		vm.alwaysShowControls()
		controlsShownAfterEnablingAlwaysShown = vm.isScreenControlsVisible.value
		bus.sendMessage(NowPlayingMessage.FilePropertiesLoaded)
		PromiseDelay.delay<Any?>(Duration.standardSeconds(6)).toExpiringFuture().get()
		controlsShownAfterTimeout = vm.isScreenControlsVisible.value
		vm.disableAlwaysShowingControls()
		controlsShownAfterDisablingAlwaysShown = vm.isScreenControlsVisible.value
	}

	@Test
	fun `then the controls are shown after enabling always shown`() {
		assertThat(controlsShownAfterEnablingAlwaysShown).isTrue
	}

	@Test
	fun `then the controls are shown after typical timeout`() {
		assertThat(controlsShownAfterTimeout).isTrue
	}

	@Test
	fun `then the controls are shown after disabling always shown`() {
		assertThat(controlsShownAfterDisablingAlwaysShown).isFalse
	}
}
