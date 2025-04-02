package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 780

class WhenLoadingTheItems {
	private val viewModel by lazy {
		val itemProvider = mockk<ProvideItems> {
			every { promiseItems(LibraryId(libraryId)) } returns listOf(
                Item(226),
                Item(877),
                Item(922),
                Item(763),
                Item(696),
			).toPromise()
		}

        ItemListViewModel(
			itemProvider,
			RecordingApplicationMessageBus(),
			mockk {
				every { promiseLibraryName(LibraryId(libraryId)) } returns "Lh33".toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(viewModel.itemValue.value).isEqualTo("Lh33")
	}

	@Test
	fun `then the view model is finished loading`() {
		assertThat(viewModel.isLoading.value).isFalse
	}

	@Test
	fun `then the loaded items are correct`() {
		assertThat(viewModel.items.value)
			.hasSameElementsAs(
				listOf(
					Item(226),
					Item(877),
					Item(922),
					Item(763),
					Item(696),
				)
			)
	}
}
