package com.lasthopesoftware.bluewater.client.stored.library.items.files.GivenAMediaFile.ThatIsInAnotherLibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.mockito.Mockito

class WhenAddingTheFile : AndroidContext() {

    override fun before() {
        FuturePromise(
            StoredFileAccess(
                ApplicationProvider.getApplicationContext(),
                Mockito.mock(GetAllStoredFilesInLibrary::class.java)
            )
                .addMediaFile(
                    Library().setId(13),
                    ServiceFile(3),
                    14,
                    "a-test-path"
                )
        )
            .get()
        val storedFileAccess = StoredFileAccess(
            ApplicationProvider.getApplicationContext(),
            Mockito.mock(GetAllStoredFilesInLibrary::class.java)
        )
        FuturePromise(
            storedFileAccess.addMediaFile(
                Library().setId(15),
                ServiceFile(3),
                14,
                "a-test-path"
            )
        ).get()
        storedFile =
            FuturePromise(storedFileAccess.getStoredFile(Library().setId(15), ServiceFile(3))).get()
    }

    @Test
    fun thenTheLibraryIdIsCorrect() {
        assertThat(storedFile!!.libraryId).isEqualTo(15)
    }

    @Test
    fun thenThisLibraryDoesNotOwnTheFile() {
        assertThat(storedFile!!.isOwner).isFalse
    }

    @Test
    fun thenTheDownloadIsMarkedComplete() {
        assertThat(storedFile!!.isDownloadComplete).isTrue
    }

    @Test
    fun thenTheStoredFileHasTheCorrectMediaFileId() {
        assertThat(storedFile!!.storedMediaId).isEqualTo(14)
    }

    @Test
    fun thenTheStoredFileHasTheCorrectPath() {
        assertThat(storedFile!!.path).isEqualTo("a-test-path")
    }

    companion object {
        private var storedFile: StoredFile? = null
    }
}
