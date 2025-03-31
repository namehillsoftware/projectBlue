package com.lasthopesoftware.bluewater.client.stored.library.items.GivenASetOfStoredItems.AndCollectionIsCancelledAfterStoredItemsAreReturned

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeDeferredStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Random
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenCollectingTheAssociatedServiceFiles {
	private val firstItemExpectedFiles = givenARandomCollectionOfFiles()
	private val secondItemExpectedFiles = givenARandomCollectionOfFiles()
	private val thirdItemExpectedFiles = givenARandomCollectionOfFiles()
	private var exception: Throwable? = null

	@BeforeAll
	fun before() {
		val storedItemAccess: FakeDeferredStoredItemAccess =
			object : FakeDeferredStoredItemAccess() {
				override val storedItems: Collection<StoredItem>
					get() = listOf(
						StoredItem(1, "1", StoredItem.ItemType.ITEM),
						StoredItem(1, "2", StoredItem.ItemType.ITEM),
						StoredItem(1, "3", StoredItem.ItemType.ITEM)
					)
			}

		val fileProvider = mockk<ProvideLibraryFiles> {
			every {
				promiseFiles(LibraryId(2), ItemId("1"))
			} returns firstItemExpectedFiles.toPromise()

			every {
				promiseFiles(LibraryId(2), ItemId("2"))
			} returns secondItemExpectedFiles.toPromise()

			every {
				promiseFiles(LibraryId(2), ItemId("3"))
			} returns thirdItemExpectedFiles.toPromise()
		}

		val serviceFileCollector = StoredItemServiceFileCollector(
            storedItemAccess,
            fileProvider
        )

		val serviceFilesPromise = serviceFileCollector.promiseServiceFilesToSync(LibraryId(2))
		serviceFilesPromise.cancel()
		storedItemAccess.resolveStoredItems()
		try {
			ExpiringFuturePromise(serviceFilesPromise)[1, TimeUnit.SECONDS]
		} catch (e: ExecutionException) {
			exception = e.cause
		}
	}

	@Test
	fun `then a cancellation exception is thrown`() {
		assertThat(exception).isInstanceOf(CancellationException::class.java)
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
