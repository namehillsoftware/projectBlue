package com.lasthopesoftware.bluewater.client.stored.library.items.GivenASetOfStoredItems

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Random
import java.util.concurrent.TimeUnit

class WhenCollectingTheAssociatedServiceFiles {
    private val firstItemExpectedFiles = givenARandomCollectionOfFiles()
	private val secondItemExpectedFiles = givenARandomCollectionOfFiles()
	private val thirdItemExpectedFiles = givenARandomCollectionOfFiles()

	private val collectedFiles by lazy {
        val storedItemAccess = mockk<AccessStoredItems> {
			every { promiseStoredItems(LibraryId(15)) } returns listOf(
				StoredItem(1, "1", StoredItem.ItemType.ITEM),
				StoredItem(1, "2", StoredItem.ItemType.ITEM),
				StoredItem(1, "3", StoredItem.ItemType.ITEM)
			).toPromise()
		}

        val fileProvider = mockk<ProvideLibraryFiles> {
			every {
				promiseFiles(LibraryId(15), ItemId("1"))
			} answers { Promise(firstItemExpectedFiles) }

			every {
				promiseFiles(LibraryId(15), ItemId("2"))
			} answers { Promise(secondItemExpectedFiles) }

			every {
				promiseFiles(LibraryId(15), ItemId("3"))
			} answers { Promise(thirdItemExpectedFiles) }
		}

        val serviceFileCollector = StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider
		)

		serviceFileCollector .promiseServiceFilesToSync(LibraryId(15)).toExpiringFuture()[1, TimeUnit.SECONDS]
    }

    @Test
    fun `then all the service files are returned`() {
        assertThat(collectedFiles).hasSameElementsAs(
			firstItemExpectedFiles.union(secondItemExpectedFiles).union(thirdItemExpectedFiles)
        )
    }

    companion object {
        private fun givenARandomCollectionOfFiles(): List<ServiceFile> {
            val random = Random()
            val floor = random.nextInt(10000)
            val ceiling = random.nextInt(10000 - floor) + floor
            return (floor..ceiling).map { ServiceFile(it.toString()) }
		}
    }
}
