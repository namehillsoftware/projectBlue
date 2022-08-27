package com.lasthopesoftware.bluewater.client.stored.library.items.GivenASetOfStoredItems

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemsChecker
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenCheckingIfAnyStoredItemsExist {
	private val isAny by lazy {
		val storedItemAccess = mockk<AccessStoredItems> {
			every { promiseStoredItems(LibraryId(10)) } returns setOf(StoredItem()).toPromise()
		}

		val storedItemsChecker = StoredItemsChecker(
			storedItemAccess,
			mockk()
		)
		ExpiringFuturePromise(
			storedItemsChecker
				.promiseIsAnyStoredItemsOrFiles(LibraryId(10))
		)[1000, TimeUnit.SECONDS]
	}

	@Test
	fun `then there are some`() {
		assertThat(isAny).isTrue
	}
}
