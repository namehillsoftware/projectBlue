package com.lasthopesoftware.bluewater.client.stored.library.items.files.GivenAMediaFile

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenAddingTheFile {

	companion object {
		private val storedFile by lazy {
			val storedFileAccess = StoredFileAccess(
				ApplicationProvider.getApplicationContext(),
				mockk()
			)
			val library = Library().setId(15)
			storedFileAccess
				.addMediaFile(
					library,
					ServiceFile(3),
					14,
					"a-test-path"
				)
				.eventually {
					storedFileAccess.getStoredFile(
						library,
						ServiceFile(3)
					)
				}
				.toFuture()
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