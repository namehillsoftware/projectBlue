package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 411
private const val itemId = 883
private const val itemValue = "can"

private var playedFileList = ""

class WhenPlayingAShuffledChildItem {

	private val viewModel by lazy {
		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(libraryId), ItemId(itemId)) } returns listOf(
				Item(495),
				Item(74),
				Item(525),
				Item(787),
				Item(460),
				Item(107),
				Item(923),
			).toPromise()
		}

		val itemStringListProvider = mockk<ProvideFileStringListForItem>().apply {
			every { promiseFileStringList(LibraryId(libraryId), ItemId(460), FileListParameters.Options.Shuffled) } returns Promise(
				"2;-1;920;388;906;356204;641;221;889;"
			)
		}

		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { startPlaylist(any<String>(), any()) } answers {
				playedFileList = firstArg()
			}
		}

		ItemListViewModel(
            itemProvider,
            mockk(relaxed = true, relaxUnitFun = true),
			mockk(),
			FakeStoredItemAccess(),
            itemStringListProvider,
            controlNowPlaying,
            mockk(),
            mockk(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(libraryId), Item(itemId, itemValue)).toExpiringFuture().get()
		viewModel.items.value[4].playShuffled().toExpiringFuture().get()
	}

	@Test
	fun `then the child items are played`() {
		assertThat(playedFileList).isEqualTo("2;-1;920;388;906;356204;641;221;889;")
	}
}
