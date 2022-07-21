package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

private val viewModel by lazy {
	val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
		every { selectedLibraryId } returns LibraryId(163).toPromise()
	}

	val itemProvider = mockk<ProvideItems>().apply {
		every { promiseItems(LibraryId(163), ItemId(826)) } returns listOf(
			Item(471),
			Item(469),
			Item(102),
			Item(890),
		).toPromise()
	}

	val storedItemAccess = mockk<AccessStoredItems>().apply {
		var isItemMarkedForSync = false
		every { toggleSync(LibraryId(163), ItemId(826), true) } answers {
			isItemMarkedForSync = true
			Unit.toPromise()
		}
		every { isItemMarkedForSync(LibraryId(163), Item(826, "moderate")) } answers { isItemMarkedForSync.toPromise() }
	}

	ItemListViewModel(
        selectedLibraryIdProvider,
        itemProvider,
		mockk(relaxed = true, relaxUnitFun = true),
		storedItemAccess,
        mockk(),
        mockk(),
		mockk(),
	)
}

class WhenSyncingTheItem {
	companion object {
		@BeforeClass
		@JvmStatic
		fun act() {
			viewModel.loadItem(Item(826, "moderate"))
			viewModel.toggleSync().toExpiringFuture().get()
		}
	}

	@Test
	fun `then item is synced`() {
		assertThat(viewModel.isSynced.value).isTrue
	}
}
