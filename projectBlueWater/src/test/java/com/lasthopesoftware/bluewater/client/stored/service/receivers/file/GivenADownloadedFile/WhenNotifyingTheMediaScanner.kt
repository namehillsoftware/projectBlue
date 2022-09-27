package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenADownloadedFile

import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.file.StoredFileMediaScannerNotifier
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class WhenNotifyingTheMediaScanner {
	private val collectedFiles by lazy {
		val storedFileAccess = mockk<AccessStoredFiles>().apply {
			every { getStoredFile(14) } returns Promise(StoredFile().setId(14).setLibraryId(22).setPath("test"))
		}

		val files = ArrayList<File>()
		val storedFileMediaScannerNotifier = StoredFileMediaScannerNotifier(storedFileAccess, files::add)
		storedFileMediaScannerNotifier.receive(14).toExpiringFuture().get()

		files
	}

	@Test
	fun thenTheMediaScannerReceivesTheCorrectFile() {
		assertThat(collectedFiles.map { it.path }).containsExactly("test")
	}
}
