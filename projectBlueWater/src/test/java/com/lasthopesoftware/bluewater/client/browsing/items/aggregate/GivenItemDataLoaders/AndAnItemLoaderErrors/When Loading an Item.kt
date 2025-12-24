package com.lasthopesoftware.bluewater.client.browsing.items.aggregate.GivenItemDataLoaders.AndAnItemLoaderErrors

import com.lasthopesoftware.bluewater.client.browsing.items.AggregateItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.MutableInteractionState
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class `When Loading an Item` {

	companion object {
		private const val libraryId = 520
		private const val itemId = "PwjfR0ffGOY"
	}

	private val mut by lazy {
		val deferredPromise = DeferredPromise<Unit>(Exception("Whoops"))

		Pair(
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
					every { loadItem(LibraryId(libraryId), Item(itemId)) } returns deferredPromise
					every { isLoading } returns MutableInteractionState(false)
				},
				mockk {
					every { loadItem(LibraryId(libraryId), Item(itemId)) } returns Promise<Unit> {
						it.awaitCancellation {
							isThirdModelCancelled = true
							it.sendRejection(Exception("Not again!"))
						}
					}
					every { isLoading } returns MutableInteractionState(false)
				},
			),
			deferredPromise
		)
	}

	private var isFirstModelCancelled = false
	private var isThirdModelCancelled = false
	private var caughtException: Exception? = null

	@BeforeAll
	fun act() {
		try {
			val (vm, promise) = mut
			val futureLoadedItem = vm.loadItem(LibraryId(libraryId), Item(itemId)).toExpiringFuture()
			promise.resolve()
			futureLoadedItem.get()
		} catch (ee: ExecutionException) {
			caughtException = ee.cause as? Exception
		}
	}

	@Test
	fun `then the caught exception is correct`() {
		assertThat(caughtException?.message).isEqualTo("Whoops")
	}

	@Test
	fun `then the first model is cancelled`() {
		assertThat(isFirstModelCancelled).isTrue
	}

	@Test
	fun `then the third model is cancelled`() {
		assertThat(isThirdModelCancelled).isTrue
	}

	@Test
	fun `then the model is not loading`() {
		assertThat(mut.first.isLoading.value).isFalse
	}
}
