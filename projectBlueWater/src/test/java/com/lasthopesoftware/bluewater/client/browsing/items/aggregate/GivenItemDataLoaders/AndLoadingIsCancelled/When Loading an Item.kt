package com.lasthopesoftware.bluewater.client.browsing.items.aggregate.GivenItemDataLoaders.AndLoadingIsCancelled

import com.lasthopesoftware.bluewater.client.browsing.items.AggregateItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

class `When Loading an Item` {

	companion object {
		private const val libraryId = 425
		private const val itemId = "U12rH63"
	}

	private val mut by lazy {
		AggregateItemViewModel(
			mockk {
				every { loadItem(LibraryId(libraryId), Item(itemId)) } returns Promise {
					it.awaitCancellation {
						isFirstModelCancelled = true
						it.sendResolution(Unit)
					}
				}

				every { isLoading } returns MutableInteractionState(false)
			},
			mockk {
				every { loadItem(LibraryId(libraryId), Item(itemId)) } returns Promise<Unit> {
					it.awaitCancellation {
						isSecondModelCancelled = true
						it.sendRejection(CancellationException(":|"))
					}
				}
				every { isLoading } returns MutableInteractionState(false)
			},
			mockk {
				every { loadItem(LibraryId(libraryId), Item(itemId)) } returns Promise<Unit> {
					it.awaitCancellation {
						isThirdModelCancelled = true
						it.sendRejection(CancellationException(":/"))
					}
				}
				every { isLoading } returns MutableInteractionState(true)
			},
		)
	}

	private var isFirstModelCancelled = false
	private var isSecondModelCancelled = false
	private var isThirdModelCancelled = false
	private var caughtException: CancellationException? = null

	@BeforeAll
	fun act() {
		try {
			val promisedLoadedItem = mut.loadItem(LibraryId(libraryId), Item(itemId))
			promisedLoadedItem.cancel()
			promisedLoadedItem.toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			caughtException = ee.cause as? CancellationException
		}
	}

	@Test
	fun `then the caught exception is correct`() {
		assertThat(caughtException).isNotNull
	}

	@Test
	fun `then the first model is cancelled`() {
		assertThat(isFirstModelCancelled).isTrue
	}

	@Test
	fun `then the second model is cancelled`() {
		assertThat(isSecondModelCancelled).isTrue
	}

	@Test
	fun `then the third model is cancelled`() {
		assertThat(isThirdModelCancelled).isTrue
	}

	@Test
	fun `then the model is loading`() {
		assertThat(mut.isLoading.value).isTrue
	}
}
