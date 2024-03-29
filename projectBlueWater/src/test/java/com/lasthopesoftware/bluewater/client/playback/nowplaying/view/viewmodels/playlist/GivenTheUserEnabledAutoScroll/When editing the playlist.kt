package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.GivenTheUserEnabledAutoScroll

import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When editing the playlist` {

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
		playlistViewModel.enableUserAutoScrolling()
		playlistViewModel.editPlaylist()
	}

	@Test
	fun `then auto scroll is disabled`() {
		assertThat(playlistViewModel.isAutoScrolling.value).isFalse()
	}

	@Test
	fun `then user auto scroll is enabled`() {
		assertThat(playlistViewModel.isUserAutoScrolling.value).isTrue()
	}

	@Test
	fun `then the playlist is being edited`() {
		assertThat(playlistViewModel.isEditingPlaylist.value).isTrue()
	}
}
