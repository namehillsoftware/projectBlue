package com.lasthopesoftware.bluewater.client.stored.library.items.files.GivenAMediaFile

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.Test
import org.mockito.Mockito

class WhenAddingTheFile : AndroidContext() {

    override fun before() {
        val storedFileAccess = StoredFileAccess(
            ApplicationProvider.getApplicationContext(),
            Mockito.mock(GetAllStoredFilesInLibrary::class.java)
        )
        val library = Library().setId(15)
        storedFile = FuturePromise(
            storedFileAccess.addMediaFile(
                library,
                ServiceFile(3),
                14,
                "a-test-path"
            )
                .eventually { v: Unit ->
                    storedFileAccess.getStoredFile(
                        library,
                        ServiceFile(3)
                    )
                }).get()
    }

    @Test
    fun thenTheLibraryIdIsCorrect() {
        AssertionsForClassTypes.assertThat(storedFile!!.libraryId).isEqualTo(15)
    }

    @Test
    fun thenThisLibraryDoesNotOwnTheFile() {
        AssertionsForClassTypes.assertThat(storedFile!!.isOwner).isFalse
    }

    @Test
    fun thenTheDownloadIsMarkedComplete() {
        AssertionsForClassTypes.assertThat(storedFile!!.isDownloadComplete).isTrue
    }

    @Test
    fun thenTheStoredFileHasTheCorrectMediaFileId() {
        AssertionsForClassTypes.assertThat(storedFile!!.storedMediaId).isEqualTo(14)
    }

    @Test
    fun thenTheStoredFileHasTheCorrectPath() {
        AssertionsForClassTypes.assertThat(storedFile!!.path).isEqualTo("a-test-path")
    }

    companion object {
        private var storedFile: StoredFile? = null
    }
}
