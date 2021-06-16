package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenACancellationWhileGettingStoredItems

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeDeferredStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class WhenSyncingTheStoredItems {


	companion object {
		private val storedFileJobResults: MutableList<StoredFile> = ArrayList()
		private val storedFileAccess = mockk<IStoredFileAccess>()

		@JvmStatic
		@BeforeClass
		fun before() {
			val deferredStoredItemAccess: FakeDeferredStoredItemAccess = object : FakeDeferredStoredItemAccess() {
				override val storedItems: Collection<StoredItem>
					get() = setOf(StoredItem(1, 14, StoredItem.ItemType.PLAYLIST))
			}
			val mockFileProvider = mockk<ProvideLibraryFiles>()
			every { mockFileProvider.promiseFiles(LibraryId(13), FileListParameters.Options.None, "Playlist/Files", "Playlist=14") } returns
					Promise(listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(4),
						ServiceFile(10)))

			every { storedFileAccess.pruneStoredFiles(any(), any()) } returns Promise.empty()
			val librarySyncHandler = LibrarySyncsHandler(
				StoredItemServiceFileCollector(
					deferredStoredItemAccess,
					mockFileProvider,
					FileListParameters.getInstance()),
				storedFileAccess,
				{ l, f -> Promise(StoredFile(l,1, f, "fake-file-name", true)) },
				{ jobs ->
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
				})
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
				override fun onComplete() {
				}
			})

			deferredStoredItemAccess.resolveStoredItems()

			countDownLatch.await(30, TimeUnit.SECONDS)
		}
	}

	@Test
	fun thenTheFilesInTheStoredItemsAreNotSynced() {
		Assertions.assertThat(storedFileJobResults).isEmpty()
	}

	@Test
	fun thenFilesAreNotPruned() {
		verify(exactly = 0) { storedFileAccess.pruneStoredFiles(any(), any()) }
	}
}
