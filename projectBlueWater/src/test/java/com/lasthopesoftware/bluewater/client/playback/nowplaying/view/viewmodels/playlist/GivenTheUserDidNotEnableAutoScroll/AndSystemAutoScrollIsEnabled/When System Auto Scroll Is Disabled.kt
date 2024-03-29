package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.GivenTheUserDidNotEnableAutoScroll.AndSystemAutoScrollIsEnabled

import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When System Auto Scroll Is Disabled` {

	private val playlistViewModel by lazy {
		NowPlayingPlaylistViewModel(
			RecordingApplicationMessageBus(),
			mockk(),
			mockk(),
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		playlistViewModel.enableSystemAutoScrolling()
		playlistViewModel.disableSystemAutoScrolling()
	}

	@Test
	fun `then auto scroll is not enabled`() {
		assertThat(playlistViewModel.isAutoScrolling.value).isFalse()
	}

	@Test
	fun `then user auto scroll is not enabled`() {
		assertThat(playlistViewModel.isUserAutoScrolling.value).isFalse()
	}
}
