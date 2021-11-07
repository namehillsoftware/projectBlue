package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenASetOfStoredItems

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenSyncingTheStoredItems {

	companion object {
		private val storedFileJobResults by lazy {
			val storedItemAccessMock = mockk<IStoredItemAccess>()
			every { storedItemAccessMock.promiseStoredItems(LibraryId(52)) } returns Promise(
				setOf(StoredItem(52, 14, StoredItem.ItemType.PLAYLIST))
			)

			val fileListParameters = FileListParameters.getInstance()
			val mockFileProvider = mockk<ProvideLibraryFiles>()
			every {
				mockFileProvider.promiseFiles(
					LibraryId(52),
					FileListParameters.Options.None,
					*fileListParameters.getFileListParameters(Playlist(14))
				)
			} returns Promise(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(4),
					ServiceFile(19),
					ServiceFile(10)
				)
			)

			val storedFileAccess = mockk<PruneStoredFiles>()
			every { storedFileAccess.pruneStoredFiles(any()) } returns Promise.empty()

			val storedFilesUpdater = mockk<UpdateStoredFiles>()
			every { storedFilesUpdater.promiseStoredFileUpdate(any(), any()) } answers {
				Promise(StoredFile(firstArg(), 1, lastArg(), "fake-file-name", true))
			}

			every { storedFilesUpdater.promiseStoredFileUpdate(any(), ServiceFile(19)) } returns Promise(
				StoredFile(LibraryId(52), 1, ServiceFile(19), "fake", true).setIsDownloadComplete(true)
			)

			val librarySyncHandler = LibrarySyncsHandler(
				StoredItemServiceFileCollector(
					storedItemAccessMock,
					mockFileProvider,
					fileListParameters
				),
				storedFileAccess,
				storedFilesUpdater
			) { jobs ->
				Observable.fromIterable(jobs).flatMap { (_, _, storedFile) ->
					Observable.just(
						StoredFileJobStatus(
							mockk(),
							storedFile,
							StoredFileJobState.Downloading
						),
						StoredFileJobStatus(
							mockk(),
							storedFile,
							StoredFileJobState.Downloaded
						)
					)
				}
			}

			librarySyncHandler.observeLibrarySync(LibraryId(52))
				.filter { j -> j.storedFileJobState == StoredFileJobState.Downloaded }
				.map { j -> j.storedFile }
				.toList()
				.blockingGet()
		}
	}

    @Test
    fun thenTheFilesInTheStoredItemsAreSynced() {
        assertThat(
            storedFileJobResults.map { obj -> obj.serviceId })
            .containsExactly(1, 2, 4, 10)
    }
}
