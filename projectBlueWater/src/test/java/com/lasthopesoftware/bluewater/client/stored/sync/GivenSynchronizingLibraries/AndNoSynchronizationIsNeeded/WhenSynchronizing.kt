package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries.AndNoSynchronizationIsNeeded

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
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
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
		StoredFile().setId(nextInt()).setServiceId(114).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(92).setLibraryId(4),
		StoredFile().setId(nextInt()).setServiceId(nextInt()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt()).setLibraryId(10),
		StoredFile().setId(nextInt()).setServiceId(nextInt()).setLibraryId(10)
	)
	private val recordingMessageBus = RecordingApplicationMessageBus()
	private var danglingFilesWerePruned = false
	private val synchronization by lazy {

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
		with(checkSync) {
			every { promiseIsSyncNeeded() } returns Promise(false)
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
	fun `then a Sync started event occurs`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<SyncStateMessage.SyncStarted>()
				.single()
		).isNotNull
	}

	@Test
	fun `then no stored file events are broadcast`() {
		assertThat(recordingMessageBus.recordedMessages.map<ApplicationMessage, Class<out ApplicationMessage>> { m -> m.javaClass })
			.doesNotContain(cls<StoredFileMessage>())
	}

	@Test
	fun `then a Sync stopped event occurs`() {
		assertThat(
			recordingMessageBus.recordedMessages
				.filterIsInstance<SyncStateMessage.SyncStopped>()
				.single()
		).isNotNull
	}

	@Test
	fun `then dangling files were still pruned`() {
		assertThat(danglingFilesWerePruned).isTrue
	}
}
