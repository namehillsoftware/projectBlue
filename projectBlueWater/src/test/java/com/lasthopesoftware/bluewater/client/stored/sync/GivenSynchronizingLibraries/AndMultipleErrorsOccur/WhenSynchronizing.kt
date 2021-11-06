package com.lasthopesoftware.bluewater.client.stored.sync.GivenSynchronizingLibraries.AndMultipleErrorsOccur

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.resources.FakeMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
		private val faultingStoredFileServiceIds = listOf(7, 92)
		private val expectedStoredFileJobs = storedFiles.filter { f -> !faultingStoredFileServiceIds.contains(f.serviceId) }
		private val fakeMessageSender = FakeMessageBus(ApplicationProvider.getApplicationContext())
	}

	override fun before() {
		val libraryProvider = mockk<ILibraryProvider>()
		every { libraryProvider.allLibraries } returns Promise(listOf(Library().setId(4)))
		val librarySyncHandler = mockk<ControlLibrarySyncs>()
		every { librarySyncHandler.observeLibrarySync(LibraryId(4)) } returns Observable.concatArrayDelayError(
			Observable
				.fromArray(*storedFiles)
				.filter { f -> f.serviceId == 92 }
				.flatMap({ f ->
					Observable.concat(
						Observable.just(
							StoredFileJobStatus(mockk(), f, StoredFileJobState.Queued),
							StoredFileJobStatus(mockk(), f, StoredFileJobState.Downloading)
						),
						Observable.error(StoredFileReadException(mockk(), f))
					)
				}, true),
			Observable
				.fromArray(*storedFiles)
				.filter { f -> !faultingStoredFileServiceIds.contains(f.serviceId) }
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
						Observable.error(StoredFileWriteException(mockk(), f))
					)
				}, true)
		)

		val synchronization = StoredFileSynchronization(
			libraryProvider,
			fakeMessageSender,
			librarySyncHandler
		)
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
	fun thenTheWriteErrorsIsBroadcast() {
		assertThat(
			fakeMessageSender.recordedIntents
				.filter { i -> StoredFileSynchronization.onFileWriteErrorEvent == i.action }
				.map { i -> i.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1) })
			.containsExactlyElementsOf(storedFiles.filter { f -> f.serviceId == 7 }.map { obj -> obj.id })
	}

	@Test
	fun thenTheReadErrorsIsBroadcast() {
		assertThat(
			fakeMessageSender.recordedIntents
				.filter { i -> StoredFileSynchronization.onFileWriteErrorEvent == i.action }
				.map { i -> i.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1) })
			.containsExactlyElementsOf(storedFiles.filter { f -> f.serviceId == 7 }.map { obj -> obj.id })
	}

	@Test
	fun thenTheStoredFilesAreBroadcastAsDownloaded() {
		assertThat(
			fakeMessageSender.recordedIntents
				.filter { i -> StoredFileSynchronization.onFileDownloadedEvent == i.action }
				.map { i -> i.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1) })
			.isSubsetOf(expectedStoredFileJobs.map { obj -> obj.id })
	}

	@Test
	fun thenASyncStoppedEventOccurs() {
		assertThat(
			fakeMessageSender.recordedIntents
				.singleOrNull { i -> StoredFileSynchronization.onSyncStopEvent == i.action })
			.isNotNull
	}
}
