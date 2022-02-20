package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfServiceFiles

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenCollectingTheAssociatedServiceFiles {

	companion object {
		private var collectedFiles: Collection<ServiceFile>? = null
		private val firstItemExpectedFiles: List<ServiceFile> = emptyList()
		private val secondItemExpectedFiles: List<ServiceFile> = emptyList()
		private val thirdItemExpectedFiles: List<ServiceFile> = emptyList()

		@BeforeClass
		@JvmStatic
		fun before() {
			val storedItemAccess: AccessStoredItems = FakeStoredItemAccess(
				StoredItem(1, 1, StoredItem.ItemType.ITEM),
				StoredItem(1, 2, StoredItem.ItemType.ITEM),
				StoredItem(1, 3, StoredItem.ItemType.ITEM))
			val fileListParameters = FileListParameters
			val fileProvider = mockk<ProvideLibraryFiles>().apply {
				every {
					promiseFiles(
						LibraryId(10),
						FileListParameters.Options.None,
						*fileListParameters.getFileListParameters(ItemId(1))
					)
				} returns Promise(firstItemExpectedFiles)

				every {
					promiseFiles(
						LibraryId(10),
						FileListParameters.Options.None,
						*fileListParameters.getFileListParameters(ItemId(2))
					)
				} returns Promise(secondItemExpectedFiles)

				every {
					promiseFiles(
						LibraryId(10),
						FileListParameters.Options.None,
						*fileListParameters.getFileListParameters(ItemId(3))
					)
				} returns Promise(thirdItemExpectedFiles)
			}

			val serviceFileCollector = StoredItemServiceFileCollector(
				storedItemAccess,
				fileProvider,
				fileListParameters
			)

			collectedFiles = serviceFileCollector
				.promiseServiceFilesToSync(LibraryId(10))
				.toFuture()[1000, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenOnlyTheFoundServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(firstItemExpectedFiles.plus(thirdItemExpectedFiles).toHashSet())
	}
}
