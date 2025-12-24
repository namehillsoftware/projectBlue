package com.lasthopesoftware.bluewater.client.browsing.items.aggregate.GivenItemDataLoaders

import com.lasthopesoftware.bluewater.client.browsing.items.AggregateItemViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Refreshing an Item` {

	private val mut by lazy {
		AggregateItemViewModel(
			mockk {
				every { promiseRefresh() } answers {
					isFirstModelRefreshed = true
					Unit.toPromise()
				}
				every { isLoading } returns MutableInteractionState(true)
			},
			mockk {
				every { promiseRefresh() } answers {
					isSecondModelRefreshed = true
					Unit.toPromise()
				}
				every { isLoading } returns MutableInteractionState(true)
			},
			mockk {
				every { promiseRefresh() } answers {
					isThirdModelRefreshed = true
					Unit.toPromise()
				}
				every { isLoading } returns MutableInteractionState(true)
			},
		)
	}

	private var isFirstModelRefreshed = false
	private var isSecondModelRefreshed = false
	private var isThirdModelRefreshed = false

	@BeforeAll
	fun act() {
		mut.promiseRefresh().toExpiringFuture().get()
	}

	@Test
	fun `then the first model is refreshed`() {
		assertThat(isFirstModelRefreshed).isTrue
	}

	@Test
	fun `then the second model is refreshed`() {
		assertThat(isSecondModelRefreshed).isTrue
	}

	@Test
	fun `then the third model is refreshed`() {
		assertThat(isThirdModelRefreshed).isTrue
	}

	@Test
	fun `then the model is loading`() {
		assertThat(mut.isLoading.value).isTrue
	}
}
