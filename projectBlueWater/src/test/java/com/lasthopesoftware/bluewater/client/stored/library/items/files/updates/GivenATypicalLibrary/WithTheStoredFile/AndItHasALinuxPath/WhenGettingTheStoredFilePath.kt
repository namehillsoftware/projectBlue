package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItHasALinuxPath

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUrisLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class WhenGettingTheStoredFilePath {
	private val filePath by lazy {
		val filePropertiesProvider = FakeFilesPropertiesProvider()
		filePropertiesProvider.addFilePropertiesToCache(
			ServiceFile(340),
			LibraryId(550),
			mapOf(
				Pair(KnownFileProperties.Artist, "screw"),
				Pair(KnownFileProperties.Album, "dog"),
				Pair(KnownFileProperties.Track, "670"),
				Pair(KnownFileProperties.Filename, "/my/linux-volume/a_filename.mp3")
			)
		)

		val storedFilePathsLookup = StoredFileUrisLookup(
			filePropertiesProvider,
			FakeLibraryRepository(
				Library(_id = 550, _syncedFileLocation = Library.SyncedFileLocation.INTERNAL)
			),
			mockk {
				every { promiseSyncDirectory(LibraryId(550)) } returns Promise(File("/lock"))
			},
			mockk(),
			mockk(),
		)

		storedFilePathsLookup
			.promiseStoredFileUri(LibraryId(550), ServiceFile(340))
			.toExpiringFuture()
			.get()
	}

	@Test
	fun thenTheFilepathIsCorrect() {
		assertThat(filePath.toString()).isEqualTo("file:/lock/screw/dog/a_filename.mp3")
	}
}
