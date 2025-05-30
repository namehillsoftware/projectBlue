package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries.AndMultipleErrorsOccur

import com.lasthopesoftware.bluewater.client.browsing.library.access.ProvideLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.CheckForSync
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.client.stored.sync.SyncStateMessage
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
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
	private val faultingStoredFileServiceIds = listOf("7", "92")
	private val expectedStoredFileJobs = storedFiles.filter { f -> !faultingStoredFileServiceIds.contains(f.serviceId) }
	private val recordingMessageBus = RecordingApplicationMessageBus()
	private val synchronization by lazy {
		val filePruner = mockk<PruneStoredFiles>()
			.apply {
				every { pruneDanglingFiles() } returns Unit.toPromise()
			}

		val libraryProvider = mockk<ProvideLibraries>()
		every { libraryProvider.promiseAllLibraries() } returns Promise(listOf(Library(id = 4)))
		val librarySyncHandler = mockk<ControlLibrarySyncs>()
		every { librarySyncHandler.observeLibrarySync(LibraryId(4)) } returns Observable.concatArrayDelayError(
			Observable
				.fromArray(*storedFiles)
				.filter { f -> f.serviceId == "92" }
				.flatMap({ f ->
					Observable.concat(
						Observable.just(
							StoredFileJobStatus(f, StoredFileJobState.Queued),
							StoredFileJobStatus(f, StoredFileJobState.Downloading)
						),
						Observable.error(StoredFileReadException(mockk(), f))
					)
				}, true),
			Observable
				.fromArray(*storedFiles)
				.filter { f -> !faultingStoredFileServiceIds.contains(f.serviceId) }
				.flatMap { f ->
					Observable.just(
						StoredFileJobStatus(f, StoredFileJobState.Queued),
						StoredFileJobStatus(f, StoredFileJobState.Downloading),
						StoredFileJobStatus(f, StoredFileJobState.Downloaded)
					)
				},
			Observable
				.fromArray(*storedFiles)
				.filter { f -> f.serviceId == "7" }
				.flatMap({ f ->
					Observable.concat(
						Observable.just(
							StoredFileJobStatus(f, StoredFileJobState.Queued),
							StoredFileJobStatus(f, StoredFileJobState.Downloading),
						),
						Observable.error(StoredFileWriteException(f, mockk()))
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
	fun act() {
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
				.map { it.storedFileId })
			.isSubsetOf(storedFiles.map { it.id })
	}

	@Test
	fun `then the write errors is broadcast`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileWriteError>()
				.map { it.storedFileId })
			.containsExactlyElementsOf(storedFiles.filter { f -> f.serviceId == "7" }.map { it.id })
	}

	@Test
	fun `then the read errors is broadcast`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileReadError>()
				.map { it.storedFileId })
			.containsExactlyElementsOf(storedFiles.filter { f -> f.serviceId == "92" }.map { it.id })
	}

	@Test
	fun `then the stored files are broadcast as downloaded`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloaded>()
				.map { it.storedFileId })
			.isSubsetOf(expectedStoredFileJobs.map { it.id })
	}

	@Test
	fun thenASyncStoppedEventOccurs() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<SyncStateMessage.SyncStopped>().singleOrNull()
		).isNotNull
	}
}
