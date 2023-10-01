package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.InAnotherLocation

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GetStoredFileUris
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

@RunWith(RobolectricTestRunner::class)
class WhenUpdatingTheFile {

	companion object {
		private var storedFile = Lazy {
			val mediaFileUriProvider = mockk<MediaFileUriProvider> {
				every { promiseUri(any(), any()) } returns Promise.empty()
			}

			val fakeLibraryRepository = FakeLibraryRepository(
				Library(
					id = 14,
					syncedFileLocation = Library.SyncedFileLocation.INTERNAL
				)
			)

			val lookupStoredFilePaths = mockk<GetStoredFileUris> {
				every { promiseStoredFileUri(LibraryId(14), ServiceFile(4)) } returns Promise(
					URI("file:/my-private-drive-1/14/artist/album/my-filename.mp3")
				)
			}

			val fakeStoredFileAccess = FakeStoredFileAccess()

			StoredFileUpdater(
				fakeStoredFileAccess,
				mediaFileUriProvider,
                fakeLibraryRepository,
				lookupStoredFilePaths,
				mockk(),
			).promiseStoredFileUpdate(LibraryId(14), ServiceFile(4)).toExpiringFuture().get()

			every { lookupStoredFilePaths.promiseStoredFileUri(LibraryId(14), ServiceFile(4)) } returns Promise(
				URI("file:/my-private-drive/14/artist/album/my-filename.mp3")
			)

			val storedFileUpdater = StoredFileUpdater(
				fakeStoredFileAccess,
				mediaFileUriProvider,
                fakeLibraryRepository,
				mockk(),
				mockk(),
			)

			storedFileUpdater
				.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4))
				.toExpiringFuture()
				.get()
		}
	}

    @Test
    fun thenTheFileIsOwnedByTheLibrary() {
        assertThat(storedFile.`object`?.isOwner).isTrue
    }

    @Test
    fun thenTheFilePathIsCorrect() {
        assertThat(storedFile.`object`?.uri)
            .isEqualTo("file:/my-private-drive-1/14/artist/album/my-filename.mp3")
    }
}
