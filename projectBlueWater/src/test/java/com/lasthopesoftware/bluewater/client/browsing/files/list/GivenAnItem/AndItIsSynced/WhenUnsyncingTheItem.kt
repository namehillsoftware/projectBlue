package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnItem.AndItIsSynced

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

	private val viewModel by lazy {
		FileListViewModel(
			mockk {
				every { promiseFiles(LibraryId(707), ItemId("501")) } returns listOf(
					ServiceFile("471"),
					ServiceFile("469"),
					ServiceFile("102"),
					ServiceFile("890"),
				).toPromise()
			},
			FakeStoredItemAccess(
				StoredItem(libraryId = 707, "501", StoredItem.ItemType.ITEM, "observe")
			),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(707), Item("501", "observe")).toExpiringFuture().get()
		viewModel.toggleSync().toExpiringFuture().get()
	}

	@Test
	fun `then item is not synced`() {
		assertThat(viewModel.isSynced.value).isFalse
	}
}
