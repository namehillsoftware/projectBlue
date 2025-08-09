package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlayingAPlaylist {

	companion object {
		private const val libraryId = 423
		private const val playlistId = "cd416d900b5c48ba84058ce6c67bbc72"
	}

	private var playedFileList = emptyList<ServiceFile>()

	private val mut by lazy {
		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { startPlaylist(LibraryId(libraryId), any<List<ServiceFile>>(), any()) } answers {
				playedFileList = arg(1)
			}
		}

		ItemPlayback(
			mockk {
				every { promiseFiles(LibraryId(libraryId), PlaylistId(playlistId)) } returns listOf(
					ServiceFile("fc"),
					ServiceFile("f9"),
					ServiceFile("2"),
					ServiceFile("f"),
					ServiceFile("1e"),
					ServiceFile("b9"),
				).toPromise()
			},
            controlNowPlaying,
		)
	}

	@BeforeAll
	fun act() {
		mut.playPlaylist(LibraryId(libraryId), PlaylistId(playlistId)).toExpiringFuture().get()
	}

	@Test
	fun `then the child items are played`() {
		assertThat(playedFileList).isEqualTo(
			listOf(
				ServiceFile("fc"),
				ServiceFile("f9"),
				ServiceFile("2"),
				ServiceFile("f"),
				ServiceFile("1e"),
				ServiceFile("b9"),
			)
		)
	}
}
