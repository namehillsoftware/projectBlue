package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenASetOfStoredItems

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class WhenSyncingTheStoredItemsAndAnErrorOccursDownloading {
	companion object {
		private val storedFileJobResults by lazy {
			val storedItemAccessMock = mockk<IStoredItemAccess>()
			every { storedItemAccessMock.promiseStoredItems(LibraryId(42)) } returns Promise(
				setOf(
					StoredItem(1, 14, StoredItem.ItemType.ITEM)
				)
			)

			val fileListParameters = FileListParameters.getInstance()
			val mockFileProvider = mockk<ProvideLibraryFiles>()
			every { mockFileProvider.promiseFiles(LibraryId(42), FileListParameters.Options.None, *fileListParameters.getFileListParameters(Item(14))) } returns Promise(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(4),
					ServiceFile(10)
				)
			)

			val storedFilesPruner = mockk<PruneStoredFiles>()
			every { storedFilesPruner.pruneDanglingFiles() } returns Unit.toPromise()
			every { storedFilesPruner.pruneStoredFiles(any()) } returns Unit.toPromise()

			val storedFilesUpdater = mockk<UpdateStoredFiles>()
			every { storedFilesUpdater.promiseStoredFileUpdate(any(), any()) } answers {
				Promise(
					StoredFile(
						firstArg(),
						1,
						secondArg(),
						"fake-file-name",
						true
					)
				)
			}

			val accessStoredFiles = mockk<AccessStoredFiles>()
			every { accessStoredFiles.markStoredFileAsDownloaded(any()) } answers { Promise(firstArg<StoredFile>()) }

			val librarySyncHandler = LibrarySyncsHandler(
				StoredItemServiceFileCollector(
					storedItemAccessMock,
					mockFileProvider,
					fileListParameters
				),
				storedFilesPruner,
				storedFilesUpdater,
				StoredFileJobProcessor(
					StoredFileSystemFileProducer(),
					accessStoredFiles,
					{ _, f ->
						if (f.serviceId == 2) Promise(IOException())
						else Promise(ByteArrayInputStream(ByteArray(0)))
					},
					{ true },
					{ true },
					{ _, _ -> })
			)

			librarySyncHandler.observeLibrarySync(LibraryId(42))
				.filter { j -> j.storedFileJobState == StoredFileJobState.Downloaded }
				.map { j -> j.storedFile }
				.toList()
				.blockingGet()
		}
	}

	@Test
	fun thenTheOtherFilesInTheStoredItemsAreSynced() {
		assertThat(storedFileJobResults.map { it.serviceId }).containsExactly(1, 4, 10)
	}
}