package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfStoredItems

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

class WhenCheckingIfAnyStoredItemsExist {
    private val isAny by lazy {
        val storedItemAccess = mockk<AccessStoredItems> {
			every { promiseStoredItems(LibraryId(13)) } returns emptyList<StoredItem>().toPromise()
		}

        val checkForAnyStoredFiles = mockk<CheckForAnyStoredFiles> {
			every { promiseIsAnyStoredFiles(any()) } returns false.toPromise()
		}

        val storedItemsChecker = StoredItemsChecker(storedItemAccess, checkForAnyStoredFiles)
        ExpiringFuturePromise(storedItemsChecker.promiseIsAnyStoredItemsOrFiles(LibraryId(13)))[1000, TimeUnit.SECONDS]
    }

    @Test
    fun `then there are not any`() {
        assertThat(isAny).isFalse
    }
}
