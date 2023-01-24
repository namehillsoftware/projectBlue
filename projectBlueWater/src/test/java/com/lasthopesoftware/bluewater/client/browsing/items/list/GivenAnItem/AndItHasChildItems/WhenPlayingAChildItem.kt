package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
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

private const val libraryId = 773
private const val itemId = 107
private const val itemValue = "height"

private var playedFileList = ""

class WhenPlayingAChildItem {
	private val viewModel by lazy {
		val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
			every { promiseSelectedLibraryId() } returns LibraryId(libraryId).toPromise()
		}

		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(libraryId), ItemId(itemId)) } returns listOf(
				Item(611),
				Item(306),
				Item(867),
				Item(623),
				Item(335),
				Item(983),
			).toPromise()
		}

		val itemStringListProvider = mockk<ProvideFileStringListForItem>().apply {
			every { promiseFileStringList(LibraryId(libraryId), ItemId(867), FileListParameters.Options.None) } returns Promise(
				"2;-1;959;191;559;815;165;"
			)
		}

		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { startPlaylist(any<String>(), any()) } answers {
				playedFileList = firstArg()
			}
		}

		ItemListViewModel(
			selectedLibraryIdProvider,
			itemProvider,
			mockk(relaxed = true, relaxUnitFun = true),
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
		viewModel.items.value[2].play().toExpiringFuture().get()
	}

	@Test
	fun `then the child items are played`() {
		assertThat(playedFileList).isEqualTo("2;-1;959;191;559;815;165;")
	}
}
