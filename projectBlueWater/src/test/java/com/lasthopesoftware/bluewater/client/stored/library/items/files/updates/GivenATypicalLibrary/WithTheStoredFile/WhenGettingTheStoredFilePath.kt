package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFilePathsLookup
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class WhenGettingTheStoredFilePath {
	companion object {
		private val filePath by lazy {
			val filePropertiesProvider = FakeFilesPropertiesProvider()
			filePropertiesProvider.addFilePropertiesToCache(
				ServiceFile(340),
				LibraryId(550),
				mapOf(
					Pair(KnownFileProperties.ARTIST, "liar"),
					Pair(KnownFileProperties.ALBUM, "whenever"),
					Pair(KnownFileProperties.TRACK, "670"),
					Pair(KnownFileProperties.FILENAME, "wish somebody.mp3")
				)
			)

			val directoryLookup = mockk<LookupSyncDirectory>()
			every { directoryLookup.promiseSyncDirectory(LibraryId(550)) } returns Promise(File("/private"))

			val privateStoredFilePaths = StoredFilePathsLookup(filePropertiesProvider, directoryLookup)

			privateStoredFilePaths
				.promiseStoredFilePath(LibraryId(550), ServiceFile(340))
				.toFuture()
				.get()
		}
	}

	@Test
	fun thenTheFilepathIsCorrect() {
		assertThat(filePath).isEqualTo("/private/liar/whenever/wish somebody.mp3")
	}
}
