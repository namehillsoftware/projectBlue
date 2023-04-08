package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenALibrary.AndAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 163

class WhenLoadingTheItems {
	private val viewModel by lazy {
		val itemProvider = mockk<ProvideItems>().apply {
			every { promiseItems(LibraryId(libraryId), ItemId(826)) } returns listOf(
                Item(55),
                Item(137),
                Item(766),
                Item(812),
			).toPromise()
		}

        ItemListViewModel(
			itemProvider,
			RecordingApplicationMessageBus(),
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(libraryId), Item(826, "leaf")).toExpiringFuture().get()
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
		assertThat(viewModel.items.value)
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
