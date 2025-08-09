package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlayingAShuffledChildItem {
	companion object {
		private const val libraryId = 411
		private const val itemId = "883"
	}

	private var playedFileList = emptyList<ServiceFile>()

	private val mut by lazy {
		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { shuffleAndStartPlaylist(LibraryId(libraryId), any<List<ServiceFile>>()) } answers {
				playedFileList = secondArg()
			}
		}

		ItemPlayback(
			mockk {
				every { promiseFiles(LibraryId(libraryId), ItemId(itemId)) } returns listOf(
					ServiceFile("920"),
					ServiceFile("388"),
					ServiceFile("906"),
					ServiceFile("356204"),
					ServiceFile("641"),
					ServiceFile("221"),
					ServiceFile("889"),
				).toPromise()
			},
            controlNowPlaying,
		)
	}

	@BeforeAll
	fun act() {
		mut.playItemShuffled(LibraryId(libraryId), ItemId(itemId)).toExpiringFuture().get()
	}

	@Test
	fun `then the child items are played`() {
		assertThat(playedFileList).hasSameElementsAs(
			listOf(
				ServiceFile("920"),
				ServiceFile("388"),
				ServiceFile("906"),
				ServiceFile("356204"),
				ServiceFile("641"),
				ServiceFile("221"),
				ServiceFile("889"),
			)
		)
	}
}
