package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSyncingTheItem {

	private val fakeStoredItemAccess by lazy { FakeStoredItemAccess() }

	private val viewModel by lazy {
		FileListViewModel(
			mockk {
				every { promiseFiles(LibraryId(163), ItemId("826")) } returns listOf(
					ServiceFile("471"),
					ServiceFile("469"),
					ServiceFile("102"),
					ServiceFile("890"),
				).toPromise()
			},
			fakeStoredItemAccess,
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(163), Item("826", "moderate")).toExpiringFuture().get()
		viewModel.toggleSync().toExpiringFuture().get()
	}

	@Test
	fun `then item is synced`() {
		assertThat(viewModel.isSynced.value).isTrue
	}

	@Test
	fun `then the item is toggled for sync correctly`() {
		assertThat(fakeStoredItemAccess.inMemoryStoredItems).isEqualTo(listOf(
			StoredItem(libraryId = 163, serviceId = "826", itemType = StoredItem.ItemType.ITEM, itemName = "moderate")
		))
	}
}
