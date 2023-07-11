package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 256

class `When loading the library items twice` {
	private val expectedItems = listOf(
		Item(330),
		Item(686),
		Item(186),
		Item(929),
	)

	private val mut by lazy {
		val deferredItems = DeferredPromise(expectedItems)

		Pair(
			deferredItems,
			ItemListViewModel(
				mockk {
					every { promiseItems(LibraryId(libraryId)) } returns listOf(
						Item(645),
						Item(820),
						Item(358),
						Item(886),
						Item(50),
					).toPromise() andThen deferredItems
				},
				RecordingApplicationMessageBus(),
				mockk {
					every { promiseLibrary(LibraryId(libraryId)) } returns Promise(
						Library(
							_id = libraryId,
							_accessCode = "Lh33",
						)
					)
				},
			)
		)
	}

	private var isLoadingAfterReload = false

	@BeforeAll
	fun act() {
		val (deferredItem, vm) = mut
		vm.loadItem(LibraryId(libraryId)).toExpiringFuture().get()

		val futureLoading = vm.loadItem(LibraryId(libraryId)).toExpiringFuture()

		isLoadingAfterReload = vm.isLoading.value
		deferredItem.resolve()

		futureLoading.get()
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(mut.second.itemValue.value).isEqualTo("Lh33")
	}

	@Test
	fun `then the view model does reflect loading when loading second item`() {
		assertThat(isLoadingAfterReload).isFalse
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
