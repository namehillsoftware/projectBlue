package com.lasthopesoftware.bluewater.client.browsing.items.aggregate.GivenItemDataLoaders

import com.lasthopesoftware.bluewater.client.browsing.items.AggregateItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Loading an Item` {

	companion object {
		private const val libraryId = 22
		private const val itemId = "w5q4DLY"
	}

	private val mut by lazy {
		AggregateItemViewModel(
			mockk {
				every { loadItem(LibraryId(libraryId), Item(itemId)) } answers {
					isFirstModelCalled = true
					Unit.toPromise()
				}
				every { isLoading } returns MutableInteractionState(false)
			},
			mockk {
				every { loadItem(LibraryId(libraryId), Item(itemId)) } answers {
					isSecondModelCalled = true
					Unit.toPromise()
				}
				every { isLoading } returns MutableInteractionState(true)
			},
			mockk {
				every { loadItem(LibraryId(libraryId), Item(itemId)) } answers {
					isThirdModelCalled = true
					Unit.toPromise()
				}
				every { isLoading } returns MutableInteractionState(true)
			},
		)
	}

	private var isFirstModelCalled = false
	private var isSecondModelCalled = false
	private var isThirdModelCalled = false

	@BeforeAll
	fun act() {
		mut.loadItem(LibraryId(libraryId), Item(itemId)).toExpiringFuture().get()
	}

	@Test
	fun `then the first model is loaded`() {
		assertThat(isFirstModelCalled).isTrue
	}

	@Test
	fun `then the second model is loaded`() {
		assertThat(isSecondModelCalled).isTrue
	}

	@Test
	fun `then the third model is loaded`() {
		assertThat(isThirdModelCalled).isTrue
	}

	@Test
	fun `then the model is loading`() {
		assertThat(mut.isLoading.value).isTrue
	}
}
