package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenACancellationWhileGettingStoredItems

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeDeferredStoredItemAccess
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
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WhenSyncingTheStoredItems {

	private val storedFileJobResults = ArrayList<StoredFile>()

	@BeforeAll
	fun before() {
		val deferredStoredItemAccess: FakeDeferredStoredItemAccess = object : FakeDeferredStoredItemAccess() {
			override val storedItems: Collection<StoredItem>
				get() = setOf(StoredItem(1, 14, StoredItem.ItemType.PLAYLIST))
		}
		val mockFileProvider = mockk<ProvideLibraryFiles>()
		every {
			mockFileProvider.promiseFiles(LibraryId(13), PlaylistId(14))
		} returns
			Promise(
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(4),
					ServiceFile(10)
				)
			)

		val pruneStoredFiles = mockk<PruneStoredFiles>()
			.apply {
				every { pruneStoredFiles(any()) } returns Unit.toPromise()
				every { pruneDanglingFiles() } returns Unit.toPromise()
			}
		val librarySyncHandler = LibrarySyncsHandler(
			StoredItemServiceFileCollector(
                deferredStoredItemAccess,
                mockFileProvider
            ),
			pruneStoredFiles,
			object : UpdateStoredFiles {
				override fun markStoredFileAsDownloaded(storedFile: StoredFile): Promise<StoredFile> = storedFile.toPromise()

				override fun promiseStoredFileUpdate(
					libraryId: LibraryId,
					serviceFile: ServiceFile
				): Promise<StoredFile> =
					Promise(StoredFile(libraryId, serviceFile, URI("fake-file-name"), true))
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

		val syncedFiles = librarySyncHandler.observeLibrarySync(LibraryId(5)).map { j -> j.storedFile }
		val countDownLatch = CountDownLatch(1)
		syncedFiles.subscribe(object : Observer<StoredFile> {
			override fun onSubscribe(d: Disposable) {
				d.dispose()
				countDownLatch.countDown()
			}

			override fun onNext(storedFile: StoredFile) {
				storedFileJobResults.add(storedFile)
			}

			override fun onError(e: Throwable) {}
			override fun onComplete() {}
		})

		deferredStoredItemAccess.resolveStoredItems()

		countDownLatch.await(30, TimeUnit.SECONDS)
	}

	@Test
	fun `then the files in the stored items are not synced`() {
		assertThat(storedFileJobResults).isEmpty()
	}
}
