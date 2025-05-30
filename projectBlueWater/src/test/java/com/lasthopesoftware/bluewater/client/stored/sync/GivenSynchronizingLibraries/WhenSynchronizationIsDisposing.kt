package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries

import com.lasthopesoftware.bluewater.client.browsing.library.access.ProvideLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.PruneStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
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

class WhenSynchronizationIsDisposing {

	private val storedFiles = arrayOf(
		StoredFile().setId(nextInt()).setServiceId("1").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("2").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("4").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("5").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("114").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId("92").setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(nextInt().toString()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt().toString()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt().toString()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt().toString()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt().toString()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt().toString()).setLibraryId(10)
	)
	private val recordingMessageBus = RecordingApplicationMessageBus()
	private val synchronization by lazy {
		val filePruner = mockk<PruneStoredFiles>()
			.apply {
				every { pruneDanglingFiles() } returns Unit.toPromise()
			}

		val libraryProvider = mockk<ProvideLibraries>()
		every { libraryProvider.promiseAllLibraries() } returns Promise(
			listOf(
				Library(id = 4),
				Library(id = 10)
			)
		)

		val librarySyncHandler = mockk<ControlLibrarySyncs>()
		every { librarySyncHandler.observeLibrarySync(any()) } answers {
			Observable
				.fromArray(*storedFiles)
				.filter { f -> f.libraryId == firstArg<LibraryId>().id }
				.flatMap { f ->
					Observable.concat(
						Observable.just(
							StoredFileJobStatus(f, StoredFileJobState.Queued),
							StoredFileJobStatus(f, StoredFileJobState.Downloading),
							StoredFileJobStatus(f, StoredFileJobState.Downloaded)
						),
						Observable.never()
					)
				}
		}

		val checkSync = mockk<CheckForSync>()
		with (checkSync) {
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
		synchronization.streamFileSynchronization().subscribe().dispose()
	}

	@Test
	fun `then a Sync started event occurs`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<SyncStateMessage.SyncStarted>()
				.singleOrNull()).isNotNull
	}

	@Test
	fun `then the stored files are broadcast as queued`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileQueued>()
				.map { it.storedFileId })
			.containsExactlyElementsOf(storedFiles.map { it.id })
	}

	@Test
	fun `then the stored files are broadcast as downloading`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloading>()
				.map { it.storedFileId })
			.containsExactlyElementsOf(storedFiles.map { it.id })
	}

	@Test
	fun `then the stored files are broadcast as downloaded`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloading>()
				.map { it.storedFileId })
			.containsExactlyElementsOf(storedFiles.map { it.id })
	}

	@Test
	fun `then a Sync stopped event occurs`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<SyncStateMessage.SyncStopped>()
				.singleOrNull()).isNotNull
	}
}
