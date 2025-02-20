package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.screen.GivenTheNowPlayingScreenIsLoaded

import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

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

	private var testStartTime = 0L

	@BeforeAll
	fun act() {
		testStartTime = System.currentTimeMillis()
		mut.first.sendMessage(NowPlayingMessage.FilePropertiesLoaded)
	}

	@Test
	@Timeout(10, unit = TimeUnit.SECONDS)
	fun `then the controls are shown at least five seconds after the properties load`() {
		mut.second.isScreenControlsVisible.skipWhile { !it.value }.takeWhile { it.value }.blockingSubscribe()
		assertThat(System.currentTimeMillis() - testStartTime).isGreaterThanOrEqualTo(5_000)
	}
}
