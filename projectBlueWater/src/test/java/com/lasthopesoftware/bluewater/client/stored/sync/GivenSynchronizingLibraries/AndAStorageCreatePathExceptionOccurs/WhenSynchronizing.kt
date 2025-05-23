package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries.AndAStorageCreatePathExceptionOccurs

import com.lasthopesoftware.bluewater.client.browsing.library.access.ProvideLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.CheckForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt

class WhenSynchronizing {

	private val storedFiles = arrayOf(
		StoredFile().setId(nextInt()).setServiceId("1").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("2").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("4").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("5").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("7").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("114").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("92").setLibraryId(4)
	)
	private val expectedStoredFileJobs = storedFiles.filter { f -> f.serviceId != "114" }
	private val applicationMessageBus = RecordingApplicationMessageBus()
	private val synchronization by lazy {
		val filePruner = mockk<PruneStoredFiles> {
			every { pruneDanglingFiles() } returns Unit.toPromise()
		}

		val libraryProvider = mockk<ProvideLibraries>()
		every { libraryProvider.promiseAllLibraries() } returns Promise(listOf(Library(id = 4)))

		val librarySyncHandler = mockk<ControlLibrarySyncs>()
		every { librarySyncHandler.observeLibrarySync(any()) } returns
			Observable.concat(
				Observable
					.fromArray(*storedFiles)
					.filter { f -> f.serviceId != "114" }
					.flatMap { f ->
						Observable.just(
							StoredFileJobStatus(f, StoredFileJobState.Queued),
							StoredFileJobStatus(f, StoredFileJobState.Downloading),
							StoredFileJobStatus(f, StoredFileJobState.Downloaded))
					},
				Observable
					.fromArray(*storedFiles)
					.filter { f -> f.serviceId == "114" }
					.flatMap({ f ->
						Observable.concat(
							Observable.just(
								StoredFileJobStatus(f, StoredFileJobState.Queued),
								StoredFileJobStatus(
                                    f,
									StoredFileJobState.Downloading)),
							Observable.error(StorageCreatePathException(mockk()))
						)
					}, true))

		val checkSync = mockk<CheckForSync> {
			every { promiseIsSyncNeeded() } returns Promise(false)
		}

		StoredFileSynchronization(
			libraryProvider,
			applicationMessageBus,
			filePruner,
			checkSync,
			librarySyncHandler
		)
	}

	@BeforeAll
	fun before() {
		synchronization.streamFileSynchronization().blockingAwait()
	}

	@Test
	fun `then the stored files are broadcast as queued`() {
		assertThat(
			applicationMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileQueued>()
				.map { it.storedFileId })
			.isSubsetOf(storedFiles.map { it.id })
	}

	@Test
	fun `then the stored files are broadcast as downloading`() {
		assertThat(
			applicationMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloading>()
				.map { it.storedFileId })
			.isSubsetOf(storedFiles.map { it.id })
	}

	@Test
	fun `then the stored files are broadcast as downloaded`() {
		assertThat(
			applicationMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloaded>()
				.map { it.storedFileId })
			.isSubsetOf(expectedStoredFileJobs.map { it.id })
	}
}
