package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
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

private const val libraryId = 411
private const val itemId = 883

class WhenPlayingAShuffledChildItem {
	private var playedFileList = ""

	private val mut by lazy {
		val itemStringListProvider = mockk<ProvideFileStringListForItem>().apply {
			every { promiseFileStringList(LibraryId(libraryId), ItemId(itemId), FileListParameters.Options.Shuffled) } returns Promise(
				"2;-1;920;388;906;356204;641;221;889;"
			)
		}

		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { startPlaylist(any<String>(), any()) } answers {
				playedFileList = firstArg()
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
