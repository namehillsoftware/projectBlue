package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenASetOfStoredItems

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI

class WhenSyncingTheStoredItems {

	private lateinit var storedFileJobResults: MutableList<StoredFile>

	private val librarySyncsHandler by lazy {
		val storedFileAccess by lazy {
			mockk<PruneStoredFiles>()
				.apply {
					every { pruneStoredFiles(any()) } returns Unit.toPromise()
				}
		}

		val storedItemAccessMock = mockk<AccessStoredItems>()
			.apply {
				every { promiseStoredItems(LibraryId(52)) } returns Promise(
					setOf(StoredItem(52, 14, StoredItem.ItemType.PLAYLIST))
				)
			}

		val fileListParameters = FileListParameters
		val mockFileProvider = mockk<ProvideLibraryFiles>()
			.apply {
				every {
					promiseFiles(
						LibraryId(52),
						FileListParameters.Options.None,
						*fileListParameters.getFileListParameters(PlaylistId(14))
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
					Promise(StoredFile(firstArg(), lastArg(), URI("fake-file-name"), true))
				}

				every { promiseStoredFileUpdate(any(), ServiceFile(19)) } returns Promise(
					StoredFile(LibraryId(52), ServiceFile(19), URI("fake"), true).setIsDownloadComplete(true)
				)
			}

		LibrarySyncsHandler(
			StoredItemServiceFileCollector(
				storedItemAccessMock,
				mockFileProvider,
				fileListParameters
			),
			storedFileAccess,
			storedFilesUpdater,
			mockk {
				every { observeStoredFileDownload(any()) } answers {
					val jobs = firstArg<Iterable<StoredFileJob>>()
					Observable.fromIterable(jobs).flatMap { (_, _, storedFile) ->
						Observable.just(
							StoredFileJobStatus(
                                storedFile,
								StoredFileJobState.Downloading
							),
							StoredFileJobStatus(
                                storedFile,
								StoredFileJobState.Downloaded
							)
						)
					}
				}
			}
		)
	}

	@BeforeAll
	fun act() {
		storedFileJobResults = librarySyncsHandler.observeLibrarySync(LibraryId(52))
			.filter { j -> j.storedFileJobState == StoredFileJobState.Downloaded }
			.map { j -> j.storedFile }
			.toList()
			.blockingGet()
	}

	@Test
	fun `then the files in the stored items are synced`() {
		assertThat(
			storedFileJobResults.map { obj -> obj.serviceId })
			.containsExactly(1, 2, 4, 10)
	}
}
