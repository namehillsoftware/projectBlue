package com.lasthopesoftware.bluewater.client.stored.library.items.files.GivenAMediaFile.ThatIsInAnotherLibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.lazyj.Lazy
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenAddingTheFile {

	companion object {
		private val storedFile = Lazy {
			StoredFileAccess(ApplicationProvider.getApplicationContext())
				.addMediaFile(
					Library().setId(13),
					ServiceFile(3),
					14,
					"a-test-path"
				)
				.toExpiringFuture()
				.get()

			val storedFileAccess = StoredFileAccess(ApplicationProvider.getApplicationContext())
			storedFileAccess
				.addMediaFile(
					Library().setId(15),
					ServiceFile(3),
					14,
					"a-test-path"
				)
				.toExpiringFuture()
				.get()
			storedFileAccess
				.getStoredFile(Library().setId(15), ServiceFile(3))
				.toExpiringFuture()
				.get()!!
		}
	}


    @Test
    fun thenTheLibraryIdIsCorrect() {
        assertThat(storedFile.`object`?.libraryId).isEqualTo(15)
    }

    @Test
    fun thenThisLibraryDoesNotOwnTheFile() {
        assertThat(storedFile.`object`?.isOwner).isFalse
    }

    @Test
    fun thenTheDownloadIsMarkedComplete() {
        assertThat(storedFile.`object`?.isDownloadComplete).isTrue
    }

    @Test
    fun thenTheStoredFileHasTheCorrectMediaFileId() {
        assertThat(storedFile.`object`?.storedMediaId).isEqualTo(14)
    }

    @Test
    fun thenTheStoredFileHasTheCorrectPath() {
        assertThat(storedFile.`object`?.path).isEqualTo("a-test-path")
    }
}
