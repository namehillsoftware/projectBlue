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
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenSyncingTheStoredItems {

	companion object {
		private lateinit var storedFileJobResults: MutableList<StoredFile>

		private val storedFileAccess by lazy {
			mockk<PruneStoredFiles>()
				.apply {
					every { pruneStoredFiles(any()) } returns Unit.toPromise()
					every { pruneDanglingFiles() } returns Unit.toPromise()
				}
		}

		private val librarySyncsHandler by lazy {
			val storedItemAccessMock = mockk<IStoredItemAccess>()
				.apply {
					every { promiseStoredItems(LibraryId(52)) } returns Promise(
						setOf(StoredItem(52, 14, StoredItem.ItemType.PLAYLIST))
					)
				}

			val fileListParameters = FileListParameters.getInstance()
			val mockFileProvider = mockk<ProvideLibraryFiles>()
				.apply {
					every {
						promiseFiles(
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
				}

			val storedFilesUpdater = mockk<UpdateStoredFiles>()
				.apply {
					every { promiseStoredFileUpdate(any(), any()) } answers {
						Promise(StoredFile(firstArg(), 1, lastArg(), "fake-file-name", true))
					}

					every { promiseStoredFileUpdate(any(), ServiceFile(19)) } returns Promise(
						StoredFile(LibraryId(52), 1, ServiceFile(19), "fake", true).setIsDownloadComplete(true)
					)
				}

			LibrarySyncsHandler(
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
		}

		@JvmStatic
		@BeforeClass
		fun setup() {
			storedFileJobResults = librarySyncsHandler.observeLibrarySync(LibraryId(52))
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

	@Test
	fun thenDanglingFilesArePruned() {
		verify { storedFileAccess.pruneDanglingFiles() }
	}
}