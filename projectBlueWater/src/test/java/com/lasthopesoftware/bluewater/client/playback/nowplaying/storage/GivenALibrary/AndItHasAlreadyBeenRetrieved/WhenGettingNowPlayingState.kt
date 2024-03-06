package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GivenALibrary.AndItHasAlreadyBeenRetrieved

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 814

class WhenGettingNowPlayingState {

	private val mut by lazy {
		NowPlayingRepository(
			mockk {
				every { promiseSelectedLibraryId() } returns Promise(LibraryId(libraryId))
			},
			mockk {
				every { promiseLibrary(LibraryId(libraryId)) } returns Library(
					id = libraryId,
					nowPlayingProgress = 435,
					nowPlayingId = 2,
					savedTracksString = "2;-1;130;32;22;",
					isRepeating = false
				).toPromise()
			},
			mockk(),
			FakeNowPlayingState().apply {
				set(
					LibraryId(libraryId),
					NowPlaying(
						LibraryId(libraryId),
						listOf(ServiceFile(780), ServiceFile(979), ServiceFile(655)),
						1,
						672,
						true
					),
				)
			}
		)
	}

	private var nowPlaying: NowPlaying? = null

	@BeforeAll
	fun act() {
		nowPlaying = mut.promiseActiveNowPlaying().toExpiringFuture().get()
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(nowPlaying?.playingFile).isEqualTo(PositionedFile(1, ServiceFile(979)))
	}

	@Test
	fun `then now playing is correct`() {
		assertThat(nowPlaying).isEqualTo(
			NowPlaying(
				LibraryId(libraryId),
				listOf(ServiceFile(780), ServiceFile(979), ServiceFile(655)),
				1,
				672,
				true
			)
		)
	}
}
