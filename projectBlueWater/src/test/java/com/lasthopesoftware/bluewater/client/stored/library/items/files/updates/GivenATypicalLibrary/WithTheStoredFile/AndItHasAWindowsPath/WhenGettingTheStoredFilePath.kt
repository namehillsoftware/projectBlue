package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItHasAWindowsPath

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFilePathsLookup
import com.lasthopesoftware.bluewater.client.stored.library.sync.LookupSyncDirectory
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
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
					Pair(KnownFileProperties.AlbumArtist, "tobacco"),
					Pair(KnownFileProperties.Album, "sign"),
					Pair(KnownFileProperties.Track, "670"),
					Pair(KnownFileProperties.Filename, """D:\aint\windows\great\for_music.mp3""")
				)
			)

			val directoryLookup = mockk<LookupSyncDirectory>()
			every { directoryLookup.promiseSyncDirectory(LibraryId(550)) } returns Promise(File("/lock"))

			StoredFilePathsLookup(filePropertiesProvider, directoryLookup)
				.promiseStoredFilePath(LibraryId(550), ServiceFile(340))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun thenTheFilepathIsCorrect() {
		assertThat(filePath).isEqualTo("/lock/tobacco/sign/for_music.mp3")
	}
}
