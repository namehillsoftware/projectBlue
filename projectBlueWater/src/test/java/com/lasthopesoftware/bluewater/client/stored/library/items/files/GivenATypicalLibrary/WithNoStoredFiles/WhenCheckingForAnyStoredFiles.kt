package com.lasthopesoftware.bluewater.client.stored.library.items.files.GivenATypicalLibrary.WithNoStoredFiles

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.CountStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFilesChecker
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class WhenCheckingForAnyStoredFiles {
	private val isAny by lazy {
		val countStoredFilesInLibrary = mockk<CountStoredFiles> {
			every { promiseStoredFilesCount(any()) } returns 0L.toPromise()
		}

		val storedFilesChecker = StoredFilesChecker(countStoredFilesInLibrary)
		storedFilesChecker.promiseIsAnyStoredFiles(LibraryId(3)).toExpiringFuture().get()
	}

	@Test
	fun `then a false result is given`() {
		assertThat(isAny).isFalse
	}
}
