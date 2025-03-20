package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class WhenUpdatingTheFile {

	companion object {
		private val storedFile = Lazy {
			val libraryId = LibraryId(705)

			val fakeLibraryRepository = FakeLibraryRepository(
				Library(id = libraryId.id, connectionSettings = Json.encodeToString(
					StoredMediaCenterConnectionSettings(
						syncedFileLocation = SyncedFileLocation.EXTERNAL,
					)
				))
			)

			val serviceFile = ServiceFile(596)

			val storedFileUpdater = StoredFileUpdater(
				FakeStoredFileAccess(),
				mockk {
					every { promiseUri(any(), any()) } returns Promise.empty()
				},
				fakeLibraryRepository,
				mockk {
					every { promiseStoredFileUri(libraryId, serviceFile) } returns Promise(
						URI("file:/my-public-drive/14/artist/album/my-filename.mp3")
					)
				},
				mockk(),
			)

			storedFileUpdater
				.promiseStoredFileUpdate(libraryId, serviceFile)
				.toExpiringFuture()
				.get(1, TimeUnit.MINUTES)
		}
	}

	@Test
	fun `then the file is owned by the library`() {
		assertThat(storedFile.`object`?.isOwner).isTrue
	}

	@Test
	fun `then the file path is correct`() {
		assertThat(storedFile.`object`?.uri).isEqualTo("file:/my-public-drive/14/artist/album/my-filename.mp3")
	}
}
