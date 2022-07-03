package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
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
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenSynchronizing {

	companion object {
		private val random = Random()
		private val storedFiles = arrayOf(
			StoredFile().setId(random.nextInt()).setServiceId(1).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(2).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(4).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(5).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(114).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(92).setLibraryId(4),
			StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
			StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
			StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
			StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
			StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10),
			StoredFile().setId(random.nextInt()).setServiceId(random.nextInt()).setLibraryId(10)
		)
		private val recordingMessageBus = RecordingApplicationMessageBus()
		private var danglingFilesWerePruned = false

		@JvmStatic
		@BeforeClass
		fun before() {
			val libraryProvider = mockk<ILibraryProvider>()
			with(libraryProvider) {
				every { allLibraries } returns Promise(listOf(Library().setId(4), Library().setId(10)))
			}

			val librarySyncHandler = mockk<ControlLibrarySyncs>()
			with(librarySyncHandler) {
				every { observeLibrarySync(any()) } answers {
					Observable
						.fromArray(*storedFiles)
						.filter { f -> f.libraryId == firstArg<LibraryId>().id }
						.flatMap { f ->
							Observable.just(
								StoredFileJobStatus(mockk(), f, StoredFileJobState.Queued),
								StoredFileJobStatus(mockk(), f, StoredFileJobState.Downloading),
								StoredFileJobStatus(mockk(), f, StoredFileJobState.Downloaded)
							)
						}
				}
			}

			val filePruner = mockk<PruneStoredFiles>()
			with(filePruner) {
				every { pruneDanglingFiles() } answers {
					danglingFilesWerePruned = true
					Unit.toPromise()
				}
			}

			val checkSync = mockk<CheckForSync>()
			with (checkSync) {
				every { promiseIsSyncNeeded() } returns Promise(true)
			}

			val synchronization = StoredFileSynchronization(
                libraryProvider,
                recordingMessageBus,
                filePruner,
                checkSync,
                librarySyncHandler
            )
			synchronization.streamFileSynchronization().blockingAwait()
		}
	}

	@Test
	fun thenASyncStartedEventOccurs() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<SyncStateMessage.SyncStarted>()
				.single()).isNotNull
	}

	@Test
	fun thenTheStoredFilesAreBroadcastAsQueued() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileQueued>()
				.map { i -> i.storedFileId })
			.containsExactlyElementsOf(storedFiles.map { obj -> obj.id })
	}

	@Test
	fun thenTheStoredFilesAreBroadcastAsDownloading() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloading>()
				.map { i -> i.storedFileId })
			.containsExactlyElementsOf(storedFiles.map { obj -> obj.id })
	}

	@Test
	fun thenTheStoredFilesAreBroadcastAsDownloaded() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<StoredFileMessage.FileDownloaded>()
				.map { i -> i.storedFileId })
			.containsExactlyElementsOf(storedFiles.map { it.id })
	}

	@Test
	fun thenASyncStoppedEventOccurs() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<SyncStateMessage.SyncStopped>()
				.single()).isNotNull
	}

	@Test
	fun thenDanglingFilesWerePruned() {
		assertThat(danglingFilesWerePruned).isTrue
	}
}
