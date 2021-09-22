package com.lasthopesoftware.bluewater.client.stored.service.receivers.file.GivenADownloadedFile

import com.annimon.stream.Stream
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.StoredFileMediaScannerNotifier
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.util.*

class WhenNotifyingTheMediaScanner {
	@Test
	fun thenTheMediaScannerReceivesTheCorrectFile() {
		assertThat(Stream.of(collectedFiles).map { obj: File -> obj.path }
			.toList()).containsExactly("test")
	}

	companion object {
		private val collectedFiles: MutableList<File> = ArrayList()

		@BeforeClass
		@JvmStatic
		fun before() {
			val storedFileAccess = Mockito.mock(
				IStoredFileAccess::class.java
			)
			Mockito.`when`(storedFileAccess.getStoredFile(14))
				.thenReturn(Promise(StoredFile().setId(14).setLibraryId(22).setPath("test")))
			val storedFileMediaScannerNotifier = StoredFileMediaScannerNotifier(
				storedFileAccess
			) { e: File -> collectedFiles.add(e) }
			FuturePromise(storedFileMediaScannerNotifier.receive(14)).get()
		}
	}
}
