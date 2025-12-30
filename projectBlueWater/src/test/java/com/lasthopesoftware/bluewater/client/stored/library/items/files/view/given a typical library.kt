package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileMessage
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.mapNotNull
import com.lasthopesoftware.observables.toCloseable
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `given a typical library` {

	private val libraryId = 809

	@Nested
	inner class `when loading files` {
		private val services by lazy {
			ActiveFileDownloadsViewModel(
				mockk {
					every { promiseDownloadingFiles() } returns Promise(
						listOf(
							StoredFile(),
							StoredFile().setLibraryId(libraryId).setId(497),
							StoredFile().setLibraryId(libraryId).setId(939),
						)
					)
				},
				RecordingApplicationMessageBus(),
				mockk {
					every { promiseIsSyncing() } returns false.toPromise()
				},
			)
		}

		@BeforeAll
		fun act() {
			services.loadActiveDownloads(LibraryId(libraryId)).toExpiringFuture().get()
		}

		@Test
		fun `then the view is not loading`() {
			assertThat(services.isLoading.value).isFalse
		}

		@Test
		fun `then the loaded files are correct`() {
			assertThat(services.syncingFiles.value.size).isEqualTo(2)
		}
	}

	@Nested
	inner class `and files are downloading`() {
		@Nested
		inner class `when loading files` {
			val downloadedFileId = 939
			val faultyWriteFileId = 665
			val faultyReadFileId = 368
			val requeuedFileId = 228
			private val downloadingFileIds = listOf(148, 132)

			private val services by lazy {
				val messageBus = RecordingApplicationMessageBus()

				Pair(
					messageBus,
					ActiveFileDownloadsViewModel(
						mockk {
							every { promiseDownloadingFiles() } returns Promise(
								listOf(
									StoredFile(),
									StoredFile().setLibraryId(libraryId).setId(497),
									StoredFile().setLibraryId(libraryId).setId(939),
									StoredFile().setLibraryId(libraryId).setId(853),
									StoredFile().setLibraryId(libraryId).setId(148),
									StoredFile().setLibraryId(libraryId).setId(faultyReadFileId),
									StoredFile().setLibraryId(libraryId).setId(872),
									StoredFile().setLibraryId(libraryId).setId(22),
									StoredFile().setLibraryId(libraryId).setId(132),
									StoredFile().setLibraryId(libraryId).setId(92),
									StoredFile().setLibraryId(libraryId).setId(faultyWriteFileId),
									StoredFile().setLibraryId(libraryId).setId(43),
									StoredFile().setLibraryId(libraryId).setId(requeuedFileId),
								)
							)
						},
						messageBus,
						mockk {
							every { promiseIsSyncing() } returns false.toPromise()
						},
					)
				)
			}

			private val processingFileStates = mutableListOf<Pair<Int, StoredFileJobState>>()

			@BeforeAll
			fun act() {
				val (messageBus, vm) = services

				vm.syncingFiles
					.mapNotNull()
					.flatMapIterable { it.map { (f, s) -> f.id to s } }
					.subscribe(processingFileStates::add)
					.toCloseable()
					.use {
						vm.loadActiveDownloads(LibraryId(libraryId)).toExpiringFuture().get()
						messageBus.sendMessage(StoredFileMessage.FileDownloading(downloadedFileId))
						messageBus.sendMessage(StoredFileMessage.FileDownloading(requeuedFileId))
						for (id in downloadingFileIds) {
							messageBus.sendMessage(StoredFileMessage.FileDownloading(id))
						}
						messageBus.sendMessage(StoredFileMessage.FileDownloading(faultyWriteFileId))
						messageBus.sendMessage(StoredFileMessage.FileDownloaded(downloadedFileId))
						messageBus.sendMessage(StoredFileMessage.FileWriteError(faultyWriteFileId))
						messageBus.sendMessage(StoredFileMessage.FileDownloading(faultyReadFileId))
						messageBus.sendMessage(StoredFileMessage.FileReadError(faultyReadFileId))
						messageBus.sendMessage(StoredFileMessage.FileQueued(requeuedFileId))
					}
			}

			@Test
			fun `then the view is not loading`() {
				assertThat(services.second.isLoading.value).isFalse
			}

			@Test
			fun `then the syncing files are correct`() {
				assertThat(services.second.syncingFiles.value.map { (f, s) -> f.id to s })
					.isEqualTo(
						downloadingFileIds.map { it to StoredFileJobState.Downloading } +
							(497 to StoredFileJobState.Queued) +
							(853 to StoredFileJobState.Queued) +
							(872 to StoredFileJobState.Queued) +
							(22 to StoredFileJobState.Queued) +
							(92 to StoredFileJobState.Queued) +
							(43 to StoredFileJobState.Queued) +
							(faultyWriteFileId to StoredFileJobState.Queued) +
							(faultyReadFileId to StoredFileJobState.Queued) +
							(requeuedFileId to StoredFileJobState.Queued)
					)
			}

			@Test
			fun `then processing file states are correct`() {
				assertThat(processingFileStates).isEqualTo(
					listOf(
						Pair(497, StoredFileJobState.Queued),
						Pair(939, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(148, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(132, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(228, StoredFileJobState.Queued),
						Pair(939, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(148, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(132, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(228, StoredFileJobState.Queued),
						Pair(939, StoredFileJobState.Downloading),
						Pair(228, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(148, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(132, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(939, StoredFileJobState.Downloading),
						Pair(228, StoredFileJobState.Downloading),
						Pair(148, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(132, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(939, StoredFileJobState.Downloading),
						Pair(228, StoredFileJobState.Downloading),
						Pair(148, StoredFileJobState.Downloading),
						Pair(132, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(939, StoredFileJobState.Downloading),
						Pair(228, StoredFileJobState.Downloading),
						Pair(148, StoredFileJobState.Downloading),
						Pair(132, StoredFileJobState.Downloading),
						Pair(665, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(228, StoredFileJobState.Downloading),
						Pair(148, StoredFileJobState.Downloading),
						Pair(132, StoredFileJobState.Downloading),
						Pair(665, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(228, StoredFileJobState.Downloading),
						Pair(148, StoredFileJobState.Downloading),
						Pair(132, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(228, StoredFileJobState.Downloading),
						Pair(148, StoredFileJobState.Downloading),
						Pair(132, StoredFileJobState.Downloading),
						Pair(368, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(228, StoredFileJobState.Downloading),
						Pair(148, StoredFileJobState.Downloading),
						Pair(132, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(148, StoredFileJobState.Downloading),
						Pair(132, StoredFileJobState.Downloading),
						Pair(497, StoredFileJobState.Queued),
						Pair(853, StoredFileJobState.Queued),
						Pair(872, StoredFileJobState.Queued),
						Pair(22, StoredFileJobState.Queued),
						Pair(92, StoredFileJobState.Queued),
						Pair(43, StoredFileJobState.Queued),
						Pair(665, StoredFileJobState.Queued),
						Pair(368, StoredFileJobState.Queued),
						Pair(228, StoredFileJobState.Queued)
					)
				)
			}
		}
	}
}
