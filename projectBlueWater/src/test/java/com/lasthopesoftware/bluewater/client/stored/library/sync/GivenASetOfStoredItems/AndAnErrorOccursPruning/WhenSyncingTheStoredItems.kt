package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenASetOfStoredItems.AndAnErrorOccursPruning

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI

class WhenSyncingTheStoredItems {

	private val librarySyncsHandler by lazy {
		val fileListParameters = FileListParameters

		LibrarySyncsHandler(
			StoredItemServiceFileCollector(
				mockk {
					every { promiseStoredItems(LibraryId(52)) } returns Promise(
						setOf(StoredItem(52, 14, StoredItem.ItemType.PLAYLIST))
					)
				},
				mockk {
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
				},
				fileListParameters
			),
			mockk {
				every { pruneStoredFiles(any()) } returns Promise(Exception("oh no!"))
			},
			mockk {
				every { promiseStoredFileUpdate(any(), any()) } answers {
					Promise(StoredFile(firstArg(), lastArg(), URI("fake-file-name"), true))
				}

				every { promiseStoredFileUpdate(any(), ServiceFile(19)) } returns Promise(
					StoredFile(LibraryId(52), ServiceFile(19), URI("fake"), true).setIsDownloadComplete(true)
				)
			},
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

	private var storedFileJobResults = emptyList<StoredFile>()

	private var exception: Throwable? = null

	@BeforeAll
	fun act() {
		try {
			storedFileJobResults = librarySyncsHandler.observeLibrarySync(LibraryId(52))
				.filter { j -> j.storedFileJobState == StoredFileJobState.Downloaded }
				.map { j -> j.storedFile }
				.toList()
				.blockingGet()
		} catch (e: RuntimeException) {
			exception = e.cause
		}
	}

	@Test
	fun `then the files in the stored items are not synced`() {
		assertThat(storedFileJobResults).isEmpty()
	}

	@Test
	fun `then the error propagates`() {
		assertThat(exception?.message).isEqualTo("oh no!")
	}
}
