package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries.AndAStorageCreatePathExceptionOccurs

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.resources.FakeMessageSender
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.util.*

class WhenSynchronizing : AndroidContext() {

	companion object {
		private val random = Random()
		private val storedFiles = arrayOf(
			StoredFile().setId(random.nextInt()).setServiceId(1).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(2).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(4).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(5).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(7).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(114).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(92).setLibraryId(4)
		)
		private val expectedStoredFileJobs = storedFiles.filter { f: StoredFile -> f.serviceId != 114 }.toList()
		private val fakeMessageSender = FakeMessageSender(ApplicationProvider.getApplicationContext())
	}

	override fun before() {
		val libraryProvider = mockk<ILibraryProvider>()
		every { libraryProvider.allLibraries } returns Promise(listOf(Library().setId(4)))

		val librarySyncHandler = mockk<ControlLibrarySyncs>()
		every { librarySyncHandler.observeLibrarySync(any()) } returns
				Observable.concat(
					Observable
						.fromArray(*storedFiles)
						.filter { f -> f.serviceId != 114 }
						.flatMap { f ->
							Observable.just(
								StoredFileJobStatus(Mockito.mock(File::class.java), f, StoredFileJobState.Queued),
								StoredFileJobStatus(Mockito.mock(File::class.java), f, StoredFileJobState.Downloading),
								StoredFileJobStatus(Mockito.mock(File::class.java), f, StoredFileJobState.Downloaded))
						},
					Observable
						.fromArray(*storedFiles)
						.filter { f -> f.serviceId == 114 }
						.flatMap({ f ->
							Observable.concat(
								Observable.just(
									StoredFileJobStatus(Mockito.mock(File::class.java), f, StoredFileJobState.Queued),
									StoredFileJobStatus(
										Mockito.mock(File::class.java),
										f,
										StoredFileJobState.Downloading)),
								Observable.error(StorageCreatePathException(Mockito.mock(File::class.java)))
							)
						}, true))

		val synchronization = StoredFileSynchronization(libraryProvider, fakeMessageSender, librarySyncHandler)
		synchronization.streamFileSynchronization().blockingAwait()
	}

	@Test
	fun thenTheStoredFilesAreBroadcastAsQueued() {
		assertThat(
			fakeMessageSender.recordedIntents
				.filter { i -> StoredFileSynchronization.onFileQueuedEvent == i.action }
				.map { i -> i.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1) })
			.isSubsetOf(storedFiles.map { obj -> obj.id })
	}

	@Test
	fun thenTheStoredFilesAreBroadcastAsDownloading() {
		assertThat(
			fakeMessageSender.recordedIntents
				.filter { i -> StoredFileSynchronization.onFileDownloadingEvent == i.action }
				.map { i -> i.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1) })
			.isSubsetOf(storedFiles.map { obj -> obj.id })
	}

	@Test
	fun thenTheStoredFilesAreBroadcastAsDownloaded() {
		assertThat(
			fakeMessageSender.recordedIntents
				.filter { i -> StoredFileSynchronization.onFileDownloadedEvent == i.action }
				.map { i -> i.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1) })
			.isSubsetOf(expectedStoredFileJobs.map { obj -> obj.id })
	}
}
