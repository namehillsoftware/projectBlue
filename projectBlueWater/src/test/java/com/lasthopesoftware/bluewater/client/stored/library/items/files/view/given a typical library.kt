package com.lasthopesoftware.bluewater.client.stored.library.items.files.view

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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
			assertThat(services.downloadingFiles.value.size).isEqualTo(2)
		}
	}
}
