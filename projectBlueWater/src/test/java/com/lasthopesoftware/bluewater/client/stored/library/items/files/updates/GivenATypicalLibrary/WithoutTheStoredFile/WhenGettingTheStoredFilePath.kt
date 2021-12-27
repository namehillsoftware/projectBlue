package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.PrivateStoredFilePathLookup
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.SharedStoredFilePathLookup
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFilePathsLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenGettingTheStoredFilePath {
	companion object {
		private val filePath by lazy {
			val libraryProvider = mockk<ILibraryProvider>()
			every { libraryProvider.getLibrary(LibraryId(600)) } returns Promise(
				Library(_id = 600, _syncedFileLocation = Library.SyncedFileLocation.INTERNAL)
			)

			val externalFiles = mockk<SharedStoredFilePathLookup>()
			every { externalFiles.promiseSharedStoredFilePath(LibraryId(600), ServiceFile(26)) } returns Promise("leather")

			val privateFiles = mockk<PrivateStoredFilePathLookup>()
			every { privateFiles.promiseStoredFilePath(LibraryId(600), ServiceFile(26)) } returns Promise("empty")

			val storedFilePaths = StoredFilePathsLookup(libraryProvider, privateFiles, externalFiles)
			storedFilePaths
				.promiseStoredFilePath(LibraryId(600), ServiceFile(26))
				.toFuture()
				.get()
		}
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(filePath).isEqualTo("empty")
	}
}
