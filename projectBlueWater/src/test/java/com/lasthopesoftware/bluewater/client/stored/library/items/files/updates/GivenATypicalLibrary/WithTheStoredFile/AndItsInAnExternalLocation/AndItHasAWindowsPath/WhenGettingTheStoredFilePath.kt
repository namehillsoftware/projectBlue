package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItsInAnExternalLocation.AndItHasAWindowsPath

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.ExternalMusicContent
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUrisLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

private const val libraryId = 550

class WhenGettingTheStoredFilePath {

	private val expectedUri = URI("test")

	private val services by lazy {
		val filePropertiesProvider = FakeFilesPropertiesProvider()
		filePropertiesProvider.addFilePropertiesToCache(
			ServiceFile(340),
			LibraryId(libraryId),
			mapOf(
				Pair(KnownFileProperties.AlbumArtist, "6YYPwSql"),
				Pair(KnownFileProperties.Album, "baKCea7AEK"),
				Pair(KnownFileProperties.Track, "704"),
				Pair(KnownFileProperties.Filename, """F:\super\backslash\paths\in_windows.flac""")
			)
		)

		val storedFilePathsLookup = StoredFileUrisLookup(
			filePropertiesProvider,
			FakeLibraryRepository(
				Library(id = libraryId, syncedFileLocation = Library.SyncedFileLocation.EXTERNAL)
			),
			mockk {
				every { promiseSyncDirectory(LibraryId(libraryId)) } returns Promise(File("/lock"))
			},
			mockk {
			  	every { promiseUri(any(), any()) } returns Promise.empty()
			},
			mockk {
				every { promiseNewContentUri(any()) } answers {
					externalMusicContent = firstArg()
					expectedUri.toPromise()
				}
			},
		)

		storedFilePathsLookup
	}

	private var externalMusicContent: ExternalMusicContent? = null
	private var newUri: URI? = null

	@BeforeAll
	fun act() {
		newUri = services
			.promiseStoredFileUri(LibraryId(libraryId), ServiceFile(340))
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the external audio content is correct`() {
		assertThat(externalMusicContent).isEqualTo(
			ExternalMusicContent(
				displayName = "in_windows.mp3",
				relativePath = "6YYPwSql/baKCea7AEK/",
			)
		)
	}

	@Test
	fun `then the new uri is correct`() {
		assertThat(newUri).isEqualTo(expectedUri)
	}
}
