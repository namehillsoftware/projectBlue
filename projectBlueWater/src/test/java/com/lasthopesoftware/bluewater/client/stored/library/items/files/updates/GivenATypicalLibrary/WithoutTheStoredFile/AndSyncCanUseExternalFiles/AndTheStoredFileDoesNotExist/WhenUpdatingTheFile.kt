package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndSyncCanUseExternalFiles.AndTheStoredFileDoesNotExist

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.ProvideMediaFileIds
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.Lazy
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI

private const val libraryId = 814

@RunWith(RobolectricTestRunner::class)
class WhenUpdatingTheFile {

	companion object {
		private val storedFile = Lazy {

			val mediaFileUriProvider = mockk<MediaFileUriProvider>()
			every { mediaFileUriProvider.promiseUri(LibraryId(libraryId), ServiceFile(4)) } returns Promise.empty()

			val mediaFileIdProvider = mockk<ProvideMediaFileIds>()
			every { mediaFileIdProvider.getMediaId(LibraryId(libraryId), ServiceFile(4)) } returns Promise.empty()

			val fakeLibraryRepository = FakeLibraryRepository(
				Library()
					.setIsUsingExistingFiles(true)
					.setId(14)
					.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
			)

			val storedFileUpdater = StoredFileUpdater(
				ApplicationProvider.getApplicationContext(),
				mediaFileUriProvider,
				mediaFileIdProvider,
				StoredFileQuery(ApplicationProvider.getApplicationContext()),
				fakeLibraryRepository,
				mockk {
					every { promiseStoredFileUri(LibraryId(libraryId), ServiceFile(4)) } returns Promise(
						URI("file:/my-public-drive/14/artist/album/my-filename.mp3")
					)
				}
			)

			storedFileUpdater.promiseStoredFileUpdate(LibraryId(libraryId), ServiceFile(4)).toExpiringFuture().get()
		}
	}

	@Test
	fun thenTheFileIsOwnedByTheLibrary() {
		assertThat(storedFile.`object`?.isOwner).isTrue
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(storedFile.`object`?.path).isEqualTo("file:/my-public-drive/14/artist/album/my-filename.mp3")
	}
}
