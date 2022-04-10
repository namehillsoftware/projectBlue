package com.lasthopesoftware.bluewater.client.stored.library.items.GivenASetOfStoredItems.AndSomeOfTheStoredItemsArePlaylists

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class WhenCollectingTheAssociatedServiceFiles {
	@Test
	fun thenAllTheServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(
			firstItemExpectedFiles
				.plus(secondItemExpectedFiles)
				.plus(thirdItemExpectedFiles)
				.plus(fourthItemExpectedFiles)
				.toSet()
		)
	}

	companion object {
		private val firstItemExpectedFiles = givenARandomCollectionOfFiles()
		private val secondItemExpectedFiles = givenARandomCollectionOfFiles()
		private val thirdItemExpectedFiles = givenARandomCollectionOfFiles()
		private val fourthItemExpectedFiles = givenARandomCollectionOfFiles()

		private val collectedFiles by lazy {
			val storedItemAccess = FakeStoredItemAccess(
				StoredItem(1, 1, StoredItem.ItemType.ITEM),
				StoredItem(1, 2, StoredItem.ItemType.ITEM),
				StoredItem(1, 3, StoredItem.ItemType.ITEM),
				StoredItem(1, 5, StoredItem.ItemType.PLAYLIST)
			)
			val fileListParameters = FileListParameters
			val fileProvider = mockk<ProvideLibraryFiles>().apply {
				every { promiseFiles(any(), any()) } returns emptyList<ServiceFile>().toPromise()
				every { promiseFiles(LibraryId(5), FileListParameters.Options.None, *fileListParameters.getFileListParameters(ItemId(1))) } returns firstItemExpectedFiles.toPromise()
				every { promiseFiles(LibraryId(5), FileListParameters.Options.None, *fileListParameters.getFileListParameters(ItemId(2))) } returns secondItemExpectedFiles.toPromise()
				every { promiseFiles(LibraryId(5), FileListParameters.Options.None, *fileListParameters.getFileListParameters(ItemId(3))) } returns thirdItemExpectedFiles.toPromise()
				every { promiseFiles(LibraryId(5), FileListParameters.Options.None, *fileListParameters.getFileListParameters(PlaylistId(5))) } returns fourthItemExpectedFiles.toPromise()
			}

			StoredItemServiceFileCollector(storedItemAccess, fileProvider, fileListParameters)
				.promiseServiceFilesToSync(LibraryId(5))
				.toExpiringFuture()[1, TimeUnit.SECONDS]
		}

		private fun givenARandomCollectionOfFiles(): List<ServiceFile> {
			val random = Random()
			val floor = random.nextInt(10000)
			val ceiling = random.nextInt(10000 - floor) + floor
			return (floor..ceiling).map(::ServiceFile)
		}
	}
}
