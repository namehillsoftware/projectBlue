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
			private val downloadingFileIds = listOf(148, 939)

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
									StoredFile().setLibraryId(libraryId).setId(872),
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

			private val downloadingFiles = mutableSetOf<StoredFile>()

			@BeforeAll
			fun act() {
				val (messageBus, vm) = services

				vm.downloadingFiles.mapNotNull().subscribe(downloadingFiles::addAll).toCloseable().use {
					vm.loadActiveDownloads(LibraryId(libraryId)).toExpiringFuture().get()
					for (id in downloadingFileIds) {
						messageBus.sendMessage(StoredFileMessage.FileDownloading(id))
					}
				}
			}

			@Test
			fun `then the view is not loading`() {
				assertThat(services.second.isLoading.value).isFalse
			}

			@Test
			fun `then the loaded files are correct`() {
				assertThat(services.second.queuedFiles.value.map { it.id }).isEqualTo(listOf(497, 853, 872))
			}

			@Test
			fun `then the downloading files are correct`() {
				assertThat(services.second.downloadingFiles.value.map { it.id }).isEqualTo(downloadingFileIds)
			}
		}
	}
}
