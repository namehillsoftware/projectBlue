package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSyncingTheItem {

	private val viewModel by lazy {
		val storedItemAccess = mockk<AccessStoredItems>().apply {
			var isItemMarkedForSync = false
			every { toggleSync(LibraryId(163), ItemId("826"), true) } answers {
				isItemMarkedForSync = true
				Unit.toPromise()
			}
			every { isItemMarkedForSync(LibraryId(163), Item("826", "moderate")) } answers { isItemMarkedForSync.toPromise() }
		}

		FileListViewModel(
			mockk {
				every { promiseFiles(LibraryId(163), ItemId("826")) } returns listOf(
					ServiceFile("471"),
					ServiceFile("469"),
					ServiceFile("102"),
					ServiceFile("890"),
				).toPromise()
			},
            storedItemAccess,
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
}
