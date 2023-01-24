package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 374
private const val itemId = 208
private const val itemValue = "reply"

class WhenSyncingAChildItem {
	private val viewModel by lazy {
		val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
			every { promiseSelectedLibraryId() } returns LibraryId(libraryId).toPromise()
		}

		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(libraryId), ItemId(itemId)) } returns listOf(
				Item(756),
				Item(639),
				Item(178),
			).toPromise()
		}

		val storedItemAccess = FakeStoredItemAccess()

		ItemListViewModel(
			selectedLibraryIdProvider,
			itemProvider,
			mockk(relaxed = true, relaxUnitFun = true),
			storedItemAccess,
			mockk(),
			mockk(),
			mockk(),
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(libraryId), Item(itemId, itemValue)).toExpiringFuture().get()
		viewModel.items.value[2].toggleSync().toExpiringFuture().get()
	}

	@Test
	fun `then the item is synced`() {
		assertThat(viewModel.items.value[2].isSynced.value).isTrue
	}
}
