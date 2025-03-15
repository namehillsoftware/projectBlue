package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfServiceFiles

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenCollectingTheAssociatedServiceFiles {

	private val firstItemExpectedFiles = emptyList<ServiceFile>()
	private val secondItemExpectedFiles = emptyList<ServiceFile>()
	private val thirdItemExpectedFiles = emptyList<ServiceFile>()

	private val collectedFiles by lazy {
		val storedItemAccess = FakeStoredItemAccess(
			StoredItem(1, 1, StoredItem.ItemType.ITEM),
			StoredItem(1, 2, StoredItem.ItemType.ITEM),
			StoredItem(1, 3, StoredItem.ItemType.ITEM))
		val fileListParameters = FileListParameters
		val fileProvider = mockk<ProvideLibraryFiles> {
			every {
				promiseFiles(LibraryId(10), ItemId(1))
			} returns Promise(firstItemExpectedFiles)

			every {
				promiseFiles(LibraryId(10), ItemId(2))
			} returns Promise(secondItemExpectedFiles)

			every {
				promiseFiles(LibraryId(10), ItemId(3))
			} returns Promise(thirdItemExpectedFiles)
		}

		val serviceFileCollector = StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider,
			fileListParameters
		)

		serviceFileCollector
			.promiseServiceFilesToSync(LibraryId(10))
			.toExpiringFuture()[1, TimeUnit.SECONDS]
	}

	@Test
	fun `then only the found service files are returned`() {
		assertThat(collectedFiles).hasSameElementsAs(firstItemExpectedFiles.plus(thirdItemExpectedFiles).toHashSet())
	}
}
