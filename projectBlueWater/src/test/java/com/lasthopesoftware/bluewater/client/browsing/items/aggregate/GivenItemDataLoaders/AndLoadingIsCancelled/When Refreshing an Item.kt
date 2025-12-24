package com.lasthopesoftware.bluewater.client.browsing.items.aggregate.GivenItemDataLoaders.AndLoadingIsCancelled

import com.lasthopesoftware.bluewater.client.browsing.items.AggregateItemViewModel
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

class `When Refreshing an Item` {

	private val mut by lazy {
		AggregateItemViewModel(
			mockk {
				every { promiseRefresh() } returns Promise {
					it.awaitCancellation {
						isFirstModelCancelled = true
						it.sendResolution(Unit)
					}
				}

				every { isLoading } returns MutableInteractionState(false)
			},
			mockk {
				every { promiseRefresh() } returns Promise<Unit> {
					it.awaitCancellation {
						isSecondModelCancelled = true
						it.sendRejection(CancellationException())
					}
				}
				every { isLoading } returns MutableInteractionState(false)
			},
		)
	}

	private var isFirstModelCancelled = false
	private var isSecondModelCancelled = false
	private var caughtException: CancellationException? = null

	@BeforeAll
	fun act() {
		try {
			val promisedLoadedItem = mut.promiseRefresh()
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
	fun `then the model is not loading`() {
		assertThat(mut.isLoading.value).isFalse
	}
}
