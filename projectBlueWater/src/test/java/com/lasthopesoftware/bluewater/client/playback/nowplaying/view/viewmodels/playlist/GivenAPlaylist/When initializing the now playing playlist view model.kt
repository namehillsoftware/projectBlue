package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.GivenAPlaylist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 299

class `When initializing the now playing playlist view model` {

	private val mut by lazy {
		NowPlayingPlaylistViewModel(
			RecordingApplicationMessageBus(),
			FakeNowPlayingRepository(
				NowPlaying(
					LibraryId(libraryId),
					listOf(
						ServiceFile(731),
						ServiceFile(29),
						ServiceFile(294),
					),
					0,
					0,
					true
				)
			),
			mockk(),
			mockk {
				every { promiseAudioPlaylistPaths(LibraryId(libraryId)) } returns Promise(
					listOf("salesman", "help", "standard")
				)
			},
		)
	}

	@BeforeAll
	fun act() {
		mut.initializeView(LibraryId(libraryId))
	}

	@Test
	fun `then the playlist paths are loaded`() {
		assertThat(mut.playlistPaths.value).containsExactly(
			"salesman", "help", "standard"
		)
	}

	@Test
	fun `then the playlist is correct`() {
		assertThat(mut.nowPlayingList.value).containsExactly(
			PositionedFile(0, ServiceFile(731)),
			PositionedFile(1, ServiceFile(29)),
			PositionedFile(2, ServiceFile(294))
		)
	}

	@Test
	fun `then is repeating is correct`() {
		assertThat(mut.isRepeating.value).isTrue
	}
}
