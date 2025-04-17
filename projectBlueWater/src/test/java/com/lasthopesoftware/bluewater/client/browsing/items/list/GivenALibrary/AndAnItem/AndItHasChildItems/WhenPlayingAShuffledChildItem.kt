package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
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

	private var playedFileList = ""

	private val mut by lazy {
		val itemStringListProvider = mockk<ProvideFileStringListForItem>().apply {
			every { promiseShuffledFileStringList(LibraryId(libraryId), ItemId(itemId)) } returns Promise(
				"2;-1;920;388;906;356204;641;221;889;"
			)
		}

		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { startPlaylist(LibraryId(libraryId), any<String>(), any()) } answers {
				playedFileList = secondArg()
			}
		}

		ItemPlayback(
            itemStringListProvider,
            controlNowPlaying,
		)
	}

	@BeforeAll
	fun act() {
		mut.playItemShuffled(LibraryId(libraryId), ItemId(itemId)).toExpiringFuture().get()
	}

	@Test
	fun `then the child items are played`() {
		assertThat(playedFileList).isEqualTo("2;-1;920;388;906;356204;641;221;889;")
	}
}
