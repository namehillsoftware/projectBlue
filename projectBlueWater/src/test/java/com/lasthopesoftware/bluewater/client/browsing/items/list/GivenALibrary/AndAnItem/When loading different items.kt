package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenALibrary.AndAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When loading different items` {
	companion object {
		private const val libraryId = 922
		private const val firstItemId = "981"
		private const val secondItemId = "210"
	}

	private val expectedItems = listOf<IItem>(
        Item("792"),
        Item("175"),
        Item("631"),
        Item("211"),
	)

	private val mut by lazy {
		val deferredItems = DeferredPromise(expectedItems)

        Pair(
			deferredItems,
            ItemListViewModel(
				mockk {
					every { promiseItems(LibraryId(libraryId), ItemId(firstItemId)) } returns listOf(
						Item("645"),
						Item("820"),
						Item("358"),
						Item("886"),
						Item("50"),
					).toPromise()

					every { promiseItems(LibraryId(libraryId), ItemId(secondItemId)) } returns deferredItems
				},
				RecordingApplicationMessageBus(),
				mockk {
					every { promiseLibraryName(LibraryId(libraryId)) } returns "Lh33".toPromise()
				}
			)
		)
	}

	private var isLoadingAfterReload = false

	@BeforeAll
	fun act() {
		val (deferredItem, vm) = mut
		vm.loadItem(LibraryId(libraryId), Item(firstItemId, "T2l9")).toExpiringFuture().get()

		val futureLoading = vm.loadItem(LibraryId(libraryId), Item(secondItemId, "0OR")).toExpiringFuture()

		isLoadingAfterReload = vm.isLoading.value
		deferredItem.resolve()

		futureLoading.get()
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(mut.second.itemValue.value).isEqualTo("0OR")
	}

	@Test
	fun `then the view model does reflect loading when loading second item`() {
        assertThat(isLoadingAfterReload).isTrue
	}

	@Test
	fun `then the view model is finished loading`() {
		assertThat(mut.second.isLoading.value).isFalse
	}

	@Test
	fun `then the loaded items are correct`() {
		assertThat(mut.second.items.value).hasSameElementsAs(expectedItems)
	}
}
