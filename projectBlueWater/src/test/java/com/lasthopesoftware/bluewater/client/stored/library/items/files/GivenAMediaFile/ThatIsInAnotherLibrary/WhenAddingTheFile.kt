package com.lasthopesoftware.bluewater.client.stored.library.items.files.GivenAMediaFile.ThatIsInAnotherLibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenAddingTheFile {

	companion object {
		private val storedFile by lazy {
			StoredFileAccess(ApplicationProvider.getApplicationContext())
				.addMediaFile(
					LibraryId(13),
					ServiceFile(3),
					14,
					"a-test-path"
				)
				.toExpiringFuture()
				.get()

			val storedFileAccess = StoredFileAccess(ApplicationProvider.getApplicationContext())
			storedFileAccess
				.addMediaFile(
					LibraryId(15),
					ServiceFile(3),
					14,
					"a-test-path"
				)
				.toExpiringFuture()
				.get()
			storedFileAccess
				.getStoredFile(LibraryId(15), ServiceFile(3))
				.toExpiringFuture()
				.get()!!
		}
	}


    @Test
    fun thenTheLibraryIdIsCorrect() {
        assertThat(storedFile.libraryId).isEqualTo(15)
    }

    @Test
    fun thenThisLibraryDoesNotOwnTheFile() {
        assertThat(storedFile.isOwner).isFalse
    }

    @Test
    fun thenTheDownloadIsMarkedComplete() {
        assertThat(storedFile.isDownloadComplete).isTrue
    }

    @Test
    fun thenTheStoredFileHasTheCorrectMediaFileId() {
        assertThat(storedFile.storedMediaId).isEqualTo(14)
    }

    @Test
    fun thenTheStoredFileHasTheCorrectPath() {
        assertThat(storedFile.path).isEqualTo("a-test-path")
    }
}
