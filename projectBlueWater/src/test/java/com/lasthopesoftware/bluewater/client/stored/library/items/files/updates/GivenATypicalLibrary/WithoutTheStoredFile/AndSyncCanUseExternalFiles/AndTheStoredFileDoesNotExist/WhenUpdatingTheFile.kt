package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndSyncCanUseExternalFiles.AndTheStoredFileDoesNotExist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
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
import java.util.concurrent.TimeUnit

private const val libraryId = 814

@RunWith(RobolectricTestRunner::class)
class WhenUpdatingTheFile {

	companion object {
		private val storedFile = Lazy {
			val fakeLibraryRepository = FakeLibraryRepository(
				Library()
					.setIsUsingExistingFiles(true)
					.setId(libraryId)
					.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
			)

			val context = ApplicationProvider.getApplicationContext<Context>()
			val storedFileUpdater = StoredFileUpdater(
				context,
				mockk {
					every { promiseUri(LibraryId(libraryId), ServiceFile(4)) } returns Promise.empty()
				},
				mockk {
					every { getMediaId(LibraryId(libraryId), ServiceFile(4)) } returns Promise.empty()
				},
				StoredFileQuery(context),
				fakeLibraryRepository,
				mockk {
					every { promiseStoredFilePath(LibraryId(libraryId), ServiceFile(4)) } returns Promise(
						"/my-public-drive/14/artist/album/my-filename.mp3"
					)
				},
				mockk(),
			)

			storedFileUpdater
				.promiseStoredFileUpdate(LibraryId(libraryId), ServiceFile(4))
				.toExpiringFuture()
				.get(1, TimeUnit.MINUTES)
		}
	}

	@Test
	fun thenTheFileIsOwnedByTheLibrary() {
		assertThat(storedFile.`object`?.isOwner).isTrue
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(storedFile.`object`?.path).isEqualTo("/my-public-drive/14/artist/album/my-filename.mp3")
	}
}
