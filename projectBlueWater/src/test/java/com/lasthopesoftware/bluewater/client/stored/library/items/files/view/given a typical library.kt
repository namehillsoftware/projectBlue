package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
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

	enum class SimpleStoredFileJobState { Queued, Downloading }

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
			assertThat(services.queuedFiles.value.size).isEqualTo(2)
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

			private val processingFileStates = mutableListOf<Pair<SimpleStoredFileJobState, Int>>()

			@BeforeAll
			fun act() {
				val (messageBus, vm) = services

				vm.downloadingFiles
					.mapNotNull()
					.flatMapIterable { files -> files.map { SimpleStoredFileJobState.Downloading to it.id } }
					.mergeWith(
						vm.queuedFiles
							.mapNotNull()
							.flatMapIterable { files -> files.map { SimpleStoredFileJobState.Queued to it.id } })
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
			fun `then the loaded files are correct`() {
				assertThat(services.second.queuedFiles.value.map { it.id }).isEqualTo(
					listOf(
						497,
						853,
						872,
						22,
						92,
						43,
						faultyWriteFileId,
						faultyReadFileId,
						requeuedFileId,
					)
				)
			}

			@Test
			fun `then the downloading files are correct`() {
				assertThat(services.second.downloadingFiles.value.map { it.id }).isEqualTo(downloadingFileIds)
			}

			@Test
			fun `then processing file states are correct`() {
				assertThat(processingFileStates).isEqualTo(
					listOf(
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 939),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 148),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 132),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Queued, 228),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 148),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 132),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Queued, 228),
						Pair(SimpleStoredFileJobState.Downloading, 939),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 148),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 132),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Downloading, 939),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 132),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Downloading, 939),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Downloading, 939),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Downloading, 132),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Downloading, 939),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Downloading, 132),
						Pair(SimpleStoredFileJobState.Downloading, 665),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Downloading, 132),
						Pair(SimpleStoredFileJobState.Downloading, 665),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Downloading, 132),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Downloading, 132),
						Pair(SimpleStoredFileJobState.Downloading, 368),
						Pair(SimpleStoredFileJobState.Downloading, 228),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Downloading, 132),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Downloading, 148),
						Pair(SimpleStoredFileJobState.Downloading, 132),
						Pair(SimpleStoredFileJobState.Queued, 497),
						Pair(SimpleStoredFileJobState.Queued, 853),
						Pair(SimpleStoredFileJobState.Queued, 872),
						Pair(SimpleStoredFileJobState.Queued, 22),
						Pair(SimpleStoredFileJobState.Queued, 92),
						Pair(SimpleStoredFileJobState.Queued, 43),
						Pair(SimpleStoredFileJobState.Queued, 665),
						Pair(SimpleStoredFileJobState.Queued, 368),
						Pair(SimpleStoredFileJobState.Queued, 228)
					)
				)
			}
		}
	}
}
