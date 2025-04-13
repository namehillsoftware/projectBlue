package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItsInAnExternalLocation.AndTheFileAlreadyExists

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUrisLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WhenGettingTheStoredFilePath {
	companion object {
		private const val libraryId = 954
		private const val serviceFileId = "745"

		private val filePath by lazy {
			val filePropertiesProvider = FakeFilesPropertiesProvider()
			filePropertiesProvider.addFilePropertiesToCache(
                ServiceFile(serviceFileId),
                LibraryId(libraryId),
				mapOf(
					Pair(NormalizedFileProperties.AlbumArtist, "sharp"),
					Pair(NormalizedFileProperties.Album, "low"),
					Pair(NormalizedFileProperties.Track, "72"),
					Pair(NormalizedFileProperties.Filename, """/mixed\path\separators/are\awesome/for_music.mp3""")
				)
			)

			val storedFilePathsLookup = StoredFileUrisLookup(
                filePropertiesProvider,
                mockk {
					every { promiseLibrarySettings(LibraryId(libraryId)) } returns Promise(
						LibrarySettings(
							libraryId = LibraryId(libraryId),
							syncedFileLocation = SyncedFileLocation.EXTERNAL,
							connectionSettings = StoredMediaCenterConnectionSettings()
						)
					)
				},
                mockk {
                    every { promiseSyncDirectory(LibraryId(libraryId)) } returns Promise(File("/lock"))
                },
				mockk {
					every {
						promiseUri(
							LibraryId(libraryId),
							ServiceFile(serviceFileId)
						)
					} returns Promise(Uri.parse("content://media/external/audio/media/bogus1"))
				},
				mockk(),
            )

			storedFilePathsLookup
				.promiseStoredFileUri(LibraryId(libraryId), ServiceFile(serviceFileId))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun thenTheFilepathIsCorrect() {
		assertThat(filePath.toString()).isEqualTo("content://media/external/audio/media/bogus1")
	}
}
