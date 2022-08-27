package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries.AndAStoredFileWriteErrorOccurs

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.CheckForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextInt

class WhenSynchronizing {

	private val storedFiles = arrayOf(
		StoredFile().setId(nextInt()).setServiceId(1).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(2).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(4).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(5).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(7).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(114).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(92).setLibraryId(4)
	)
	private val expectedStoredFileJobs = storedFiles.filter { f: StoredFile -> f.serviceId != 7 }
	private val recordingMessageBus = RecordingApplicationMessageBus()
	private val synchronization by lazy {
		val filePruner = mockk<PruneStoredFiles>()
			.apply {
				every { pruneDanglingFiles() } returns Unit.toPromise()
			}

		val libraryProvider = mockk<ILibraryProvider>()
		every { libraryProvider.allLibraries } returns Promise(listOf(Library().setId(4)))

		val librarySyncHandler = mockk<ControlLibrarySyncs>()
		every { librarySyncHandler.observeLibrarySync(any()) } returns Observable.concat(
			Observable
				.fromArray(*storedFiles)
				.filter { f -> f.serviceId != 7 }
				.flatMap { f ->
					Observable.just(
						StoredFileJobStatus(mockk(), f, StoredFileJobState.Queued),
						StoredFileJobStatus(mockk(), f, StoredFileJobState.Downloading),
						StoredFileJobStatus(mockk(), f, StoredFileJobState.Downloaded)
					)
				},
			Observable
				.fromArray(*storedFiles)
				.filter { f -> f.serviceId == 7 }
				.flatMap({ f ->
					Observable.concat(
						Observable.just(
							StoredFileJobStatus(mockk(), f, StoredFileJobState.Queued),
							StoredFileJobStatus(mockk(), f, StoredFileJobState.Downloading),
						),
						Observable.error(
							StoredFileWriteException(mockk(), f)
						)
					)
				}, true)
		)

		val checkSync = mockk<CheckForSync>()
		with(checkSync) {
			every { promiseIsSyncNeeded() } returns Promise(true)
		}

		StoredFileSynchronization(
			libraryProvider,
			recordingMessageBus,
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
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileQueued>()
				.map { it.storedFileId })
			.isSubsetOf(storedFiles.map { it.id })
	}

	@Test
	fun `then the stored files are broadcast as downloading`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloading>()
				.map { i -> i.storedFileId })
			.isSubsetOf(storedFiles.map { it.id })
	}

	@Test
	fun `then the write errors is broadcast`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileWriteError>()
				.map { i -> i.storedFileId })
			.containsExactlyElementsOf(storedFiles.filter { f -> f.serviceId == 7 }.map { it.id })
	}

	@Test
	fun `then the stored files are broadcast as downloaded`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloaded>()
				.map { it.storedFileId })
			.isSubsetOf(expectedStoredFileJobs.map { it.id })
	}
}
