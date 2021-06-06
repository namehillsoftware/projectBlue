package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfServiceFiles

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenCollectingTheAssociatedServiceFiles {

	companion object {
		private var collectedFiles: Collection<ServiceFile>? = null
		private val firstItemExpectedFiles: List<ServiceFile> = emptyList()
		private val secondItemExpectedFiles: List<ServiceFile> = emptyList()
		private val thirdItemExpectedFiles: List<ServiceFile> = emptyList()

		@BeforeClass
		@JvmStatic
		@Throws(InterruptedException::class, TimeoutException::class, ExecutionException::class)
		fun before() {
			val storedItemAccess: IStoredItemAccess = FakeStoredItemAccess(
				StoredItem(1, 1, StoredItem.ItemType.ITEM),
				StoredItem(1, 2, StoredItem.ItemType.ITEM),
				StoredItem(1, 3, StoredItem.ItemType.ITEM))
			val fileListParameters = FileListParameters.getInstance()
			val fileProvider = mock(ProvideLibraryFiles::class.java)
			`when`(
				fileProvider.promiseFiles(
					LibraryId(10), FileListParameters.Options.None, *fileListParameters.getFileListParameters(
						Item(1)
					)
				)
			).thenAnswer { Promise(firstItemExpectedFiles) }
			`when`(
				fileProvider.promiseFiles(
					LibraryId(10), FileListParameters.Options.None, *fileListParameters.getFileListParameters(
						Item(2)
					)
				)
			).thenAnswer { Promise(secondItemExpectedFiles) }
			`when`(
				fileProvider.promiseFiles(
					LibraryId(10), FileListParameters.Options.None, *fileListParameters.getFileListParameters(
						Item(3)
					)
				)
			).thenAnswer { Promise(thirdItemExpectedFiles) }

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
