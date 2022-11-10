package com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GivenALibrary.AndItHasAlreadyBeenRetrieved

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingNowPlayingState {

	private val mut by lazy {
		NowPlayingRepository(
			mockk {
				every { promiseLibrary() } returns Library(
					_id = 521,
					_nowPlayingProgress = 435,
					_nowPlayingId = 2,
					_savedTracksString = "2;-1;130;32;22;",
					_isRepeating = false
				).toPromise()
			},
			mockk(),
			FakeNowPlayingState().apply {
				set(
					LibraryId(521),
					NowPlaying(
						LibraryId(521),
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
		nowPlaying = mut.promiseNowPlaying().toExpiringFuture().get()
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(nowPlaying?.playingFile).isEqualTo(PositionedFile(1, ServiceFile(979)))
	}

	@Test
	fun `then now playing is correct`() {
		assertThat(nowPlaying).isEqualTo(
			NowPlaying(
				LibraryId(521),
				listOf(ServiceFile(780), ServiceFile(979), ServiceFile(655)),
				1,
				672,
				true
			)
		)
	}
}
