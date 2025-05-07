package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.GivenTheUserDidNotEnableAutoScroll.AndSystemAutoScrollIsEnabled.AndEditingThePlaylist

import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When finishing playlist editing` {

	private val playlistViewModel by lazy {
		NowPlayingPlaylistViewModel(
			RecordingApplicationMessageBus(),
			mockk(),
			mockk(),
			mockk(),
		)
	}

	private var finishSucceeded = false

	@BeforeAll
	fun act() {
		playlistViewModel.enableSystemAutoScrolling()
		playlistViewModel.editPlaylist()
		finishSucceeded = playlistViewModel.finishPlaylistEdit()
	}

	@Test
	fun `then auto scroll is enabled`() {
		assertThat(playlistViewModel.isAutoScrolling.value).isTrue()
	}

	@Test
	fun `then user auto scroll is disabled`() {
		assertThat(playlistViewModel.isUserAutoScrolling.value).isFalse()
	}

	@Test
	fun `then the playlist is not being edited`() {
		assertThat(playlistViewModel.isEditingPlaylist.value).isFalse()
	}

	@Test
	fun `then the playlist cannot be saved`() {
		assertThat(playlistViewModel.isSavingPlaylistActive.value).isFalse()
	}

	@Test
	fun `then finish succeeded`() {
		assertThat(finishSucceeded).isTrue
	}
}
