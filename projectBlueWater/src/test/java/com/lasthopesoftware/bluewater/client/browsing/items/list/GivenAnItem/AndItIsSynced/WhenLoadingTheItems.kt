package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenAnItem.AndItIsSynced

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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingTheItems {

	private val viewModel by lazy {
		val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
			every { promiseSelectedLibraryId() } returns LibraryId(163).toPromise()
		}

		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(163), ItemId(826)) } returns listOf(
				Item(55),
				Item(137),
				Item(766),
				Item(812),
			).toPromise()
		}

		val storedItemAccess = mockk<AccessStoredItems>().apply {
			every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
			every { isItemMarkedForSync(LibraryId(163), Item(826, "leaf")) } returns true.toPromise()
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

	@BeforeAll
	fun act() {
		viewModel.loadItem(Item(826, "leaf")).toExpiringFuture().get()
	}

	@Test
	fun `then the item is marked for sync`() {
		assertThat(viewModel.isSynced.value).isTrue
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(viewModel.itemValue.value).isEqualTo("leaf")
	}

	@Test
	fun `then the view model is finished loading`() {
		assertThat(viewModel.isLoading.value).isFalse
	}

	@Test
	fun `then the loaded files are correct`() {
		assertThat(viewModel.items.value.map { it.item })
			.hasSameElementsAs(
				listOf(
					Item(55),
					Item(137),
					Item(766),
					Item(812),
				)
			)
	}
}
