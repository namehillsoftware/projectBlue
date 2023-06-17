package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.GivenAPlaylist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 155

class `When saving the now playing playlist` {

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
            mockk(),
			mockk {
				every { promiseAudioPlaylistPaths(LibraryId(libraryId)) } returns Promise(
					listOf("country", "hardly", "company\\pretense", "briBery\\new")
				)
				every { promiseStoredPlaylist(LibraryId(libraryId), any(), any()) } answers {
					savedPlaylistPath = secondArg()
					savedFiles.addAll(lastArg())
					Unit.toPromise()
				}
			},
        )
	}

	private var savedPlaylistPath: String? = null
	private var savedFiles = mutableListOf<ServiceFile>()

	@BeforeAll
	fun act() {
		mut.initializeView(LibraryId(libraryId)).toExpiringFuture().get()
		mut.updateSelectedPlaylistPath("bribe")
		mut.savePlaylist().toExpiringFuture().get()
	}

	@Test
	fun `then the playlist path is valid`() {
		assertThat(mut.isPlaylistPathValid.value).isTrue
	}

	@Test
	fun `then the playlist paths are loaded`() {
		assertThat(mut.filteredPlaylistPaths.value).containsExactly("briBery\\new")
	}

	@Test
	fun `then the playlist is saved to the correct path`() {
		assertThat(savedPlaylistPath).isEqualTo("bribe")
	}

	@Test
	fun `then the playlist is saved correctly`() {
		assertThat(savedFiles).containsExactly(
			ServiceFile(312),
			ServiceFile(851),
			ServiceFile(780),
			ServiceFile(650),
		)
	}

	@Test
	fun `then the playlist is correct`() {
		assertThat(mut.nowPlayingList.value).containsExactly(
            PositionedFile(0, ServiceFile(312)),
            PositionedFile(1, ServiceFile(851)),
            PositionedFile(2, ServiceFile(780)),
            PositionedFile(3, ServiceFile(650)),
		)
	}

	@Test
	fun `then is repeating is correct`() {
		assertThat(mut.isRepeating.value).isFalse
	}
}
