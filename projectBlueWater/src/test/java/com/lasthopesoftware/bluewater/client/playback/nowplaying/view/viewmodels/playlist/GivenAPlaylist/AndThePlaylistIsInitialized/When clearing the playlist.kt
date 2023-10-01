package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.GivenAPlaylist.AndThePlaylistIsInitialized

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 851

class `When clearing the playlist` {
	private val mut by lazy {
		NowPlayingPlaylistViewModel(
			RecordingApplicationMessageBus(),
			FakeNowPlayingRepository(
				NowPlaying(
					LibraryId(libraryId),
					listOf(
						ServiceFile(312),
						ServiceFile(851),
						ServiceFile(780),
						ServiceFile(650),
					),
					0,
					0,
					false
				)
			),
			mockk {
				every { clearPlaylist(any()) } answers {
					clearedPlaylistLibraryId = firstArg()
				}
			},
			mockk {
				every { promiseAudioPlaylistPaths(LibraryId(libraryId)) } returns Promise(emptyList())
			},
		)
	}

	private var clearedPlaylistLibraryId: LibraryId? = null

	@BeforeAll
	fun act() {
		mut.initializeView(LibraryId(libraryId)).toExpiringFuture().get()
		mut.clearPlaylist().toExpiringFuture().get()
	}

	@Test
	fun `then the playlist is cleared for the correct library`() {
		assertThat(clearedPlaylistLibraryId).isEqualTo(LibraryId(libraryId))
	}
}
