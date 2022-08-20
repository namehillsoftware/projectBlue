package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfStoredItems.ButStoredFilesExist

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemsChecker
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CheckForAnyStoredFiles
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenCheckingIfAnyStoredItemsOrFilesExist {
	private val isAny by lazy {
		val storedItemAccess = mockk<AccessStoredItems> {
			every { promiseStoredItems(LibraryId(10)) } returns emptyList<StoredItem>().toPromise()
		}

		val checkForAnyStoredFiles = mockk<CheckForAnyStoredFiles> {
			every { promiseIsAnyStoredFiles(any()) } returns true.toPromise()
		}

		val storedItemsChecker = StoredItemsChecker(storedItemAccess, checkForAnyStoredFiles)
		ExpiringFuturePromise(storedItemsChecker.promiseIsAnyStoredItemsOrFiles(LibraryId(10)))[1, TimeUnit.SECONDS]
	}

	@Test
	fun `then the correct result is returned`() {
		assertThat(isAny).isTrue
	}
}
