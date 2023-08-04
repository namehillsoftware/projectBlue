package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
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
		private val storedFile by lazy {
			val fakeLibraryRepository = FakeLibraryRepository(
				Library().setId(14).setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
			)

			val storedFileUpdater = StoredFileUpdater(
				ApplicationProvider.getApplicationContext(),
				mockk {
					every { promiseUri(any(), any()) } returns Promise.empty()
				},
				mockk() {
					every { getMediaId(any(), any()) } returns Promise.empty()
				},
				StoredFileQuery(ApplicationProvider.getApplicationContext()),
				fakeLibraryRepository,
				mockk {
					every { promiseStoredFileUri(LibraryId(14), ServiceFile(4)) } returns Promise(
						URI("file:///my-public-drive/14/artist/album/my-filename.mp3")
					)
				},
			)

			storedFileUpdater.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4)).toExpiringFuture().get()

			storedFileUpdater
				.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4))
				.toExpiringFuture()
				.get()
		}
	}

	@Test
	fun thenTheFileIsOwnedByTheLibrary() {
		assertThat(storedFile?.isOwner).isTrue
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(storedFile?.path)
			.isEqualTo("file:///my-public-drive/14/artist/album/my-filename.mp3")
	}
}
