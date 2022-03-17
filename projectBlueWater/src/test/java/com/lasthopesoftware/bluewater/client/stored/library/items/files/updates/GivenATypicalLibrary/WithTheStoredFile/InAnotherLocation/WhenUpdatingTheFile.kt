package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.InAnotherLocation

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.ProvideMediaFileIds
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GetStoredFilePaths
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test

class WhenUpdatingTheFile : AndroidContext() {

	companion object {
		private var storedFile: StoredFile? = null
	}

    override fun before() {
		val mediaFileUriProvider = mockk<MediaFileUriProvider>()
		every { mediaFileUriProvider.promiseFileUri(any()) } returns Promise.empty()

		val mediaFileIdProvider = mockk<ProvideMediaFileIds>()
		every { mediaFileIdProvider.getMediaId(any(), any()) } returns Promise.empty()

        val fakeLibraryProvider = FakeLibraryProvider(
            Library().setId(14).setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
        )

		val lookupStoredFilePaths = mockk<GetStoredFilePaths>()
		every { lookupStoredFilePaths.promiseStoredFilePath(LibraryId(14), ServiceFile(4)) } returns Promise("/my-public-drive-1/14/artist/album/my-filename.mp3")

		StoredFileUpdater(
			ApplicationProvider.getApplicationContext(),
			mediaFileUriProvider,
			mediaFileIdProvider,
			StoredFileQuery(ApplicationProvider.getApplicationContext()),
			fakeLibraryProvider,
			lookupStoredFilePaths
		).promiseStoredFileUpdate(LibraryId(14), ServiceFile(4)).toFuture().get()

		val lookupOtherStoredFilePaths = mockk<GetStoredFilePaths>()
		every { lookupStoredFilePaths.promiseStoredFilePath(LibraryId(14), ServiceFile(4)) } returns Promise("/my-public-drive/14/artist/album/my-filename.mp3")

        val storedFileUpdater = StoredFileUpdater(
			ApplicationProvider.getApplicationContext(),
			mediaFileUriProvider,
			mediaFileIdProvider,
			StoredFileQuery(ApplicationProvider.getApplicationContext()),
			fakeLibraryProvider,
			lookupOtherStoredFilePaths
		)
        storedFile =
            storedFileUpdater
				.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4))
        		.toFuture()
				.get()
    }

    @Test
    fun thenTheFileIsOwnedByTheLibrary() {
        assertThat(storedFile!!.isOwner).isTrue
    }

    @Test
    fun thenTheFilePathIsCorrect() {
        assertThat(storedFile!!.path)
            .isEqualTo("/my-public-drive-1/14/artist/album/my-filename.mp3")
    }
}
