package com.lasthopesoftware.bluewater.client.browsing.items.list.GivenALibrary.AndAnItem.AndItIsLoading

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class `When Cancelling` {

	companion object {
		private const val libraryId = 163
		private const val itemId = "jv3FReduzmX"
	}

	private val viewModel by lazy {
        ItemListViewModel(
			mockk {
				every { promiseItems(LibraryId(libraryId), ItemId(itemId)) } returns Promise {
					it.awaitCancellation {
						it.sendRejection(CancellationException("We're done here"))
					}
				}
			},
			RecordingApplicationMessageBus(),
			mockk(),
		)
	}

	private var cancellationException: CancellationException? = null

	@BeforeAll
	fun act() {
		try {
			val promisedLoad = viewModel.loadItem(LibraryId(libraryId), Item(itemId, "KenjiNie"))
			promisedLoad.cancel()
			promisedLoad.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			cancellationException = ee.cause as? CancellationException
		}
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(viewModel.itemValue.value).isEqualTo("KenjiNie")
	}

	@Test
	fun `then the view model is finished loading`() {
		assertThat(viewModel.isLoading.value).isFalse
	}

	@Test
	fun `then loading items is cancelled`() {
		assertThat(cancellationException).isNotNull
	}
}
