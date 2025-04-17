package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
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

	private var playedFileList = ""

	private val mut by lazy {
		val itemStringListProvider = mockk<ProvideFileStringListForItem>().apply {
			every { promiseFileStringList(LibraryId(libraryId), PlaylistId(playlistId)) } returns Promise(
				"2;-1;fc;f9;2;f;1e;b9;"
			)
		}

		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { startPlaylist(LibraryId(libraryId), any<String>(), any()) } answers {
				playedFileList = arg(1)
			}
		}

		ItemPlayback(
            itemStringListProvider,
            controlNowPlaying,
		)
	}

	@BeforeAll
	fun act() {
		mut.playPlaylist(LibraryId(libraryId), PlaylistId(playlistId)).toExpiringFuture().get()
	}

	@Test
	fun `then the child items are played`() {
		assertThat(playedFileList).isEqualTo("2;-1;fc;f9;2;f;1e;b9;")
	}
}
