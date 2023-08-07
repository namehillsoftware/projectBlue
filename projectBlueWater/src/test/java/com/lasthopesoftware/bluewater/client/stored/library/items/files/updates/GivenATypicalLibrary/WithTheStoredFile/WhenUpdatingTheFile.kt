package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.Lazy
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.*
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class WhenUpdatingTheFile {

	companion object {
		private val storedFile = Lazy {
			val libraryId = LibraryId(705)

			val fakeLibraryRepository = FakeLibraryRepository(
				Library().setId(libraryId.id).setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
			)

			val serviceFile = ServiceFile(596)

			val context = ApplicationProvider.getApplicationContext<Context>()
			val storedFileUpdater = StoredFileUpdater(
				context,
				mockk {
					every { promiseUri(any(), any()) } returns Promise.empty()
				},
				mockk {
					every { getMediaId(any(), any()) } returns Promise.empty()
				},
				StoredFileQuery(context),
				fakeLibraryRepository,
				mockk {
					every { promiseStoredFilePath(libraryId, serviceFile) } returns Promise(
						"/my-public-drive/14/artist/album/my-filename.mp3"
					)
				},
				mockk {
					every { promiseCreatedItem(libraryId, serviceFile) } returns 643.toPromise()
				},
			)

			storedFileUpdater
				.promiseStoredFileUpdate(libraryId, serviceFile)
				.toExpiringFuture()
				.get(1, TimeUnit.MINUTES)
		}
	}

	@Test
	fun `then the file is owned by the library`() {
		assertThat(storedFile.`object`?.isOwner).isTrue
	}

	@Test
	fun `then the file path is correct`() {
		assertThat(storedFile.`object`?.path).isNullOrEmpty()
	}

	@Test
	fun `then the stored media id is correct`() {
		assertThat(storedFile.`object`?.storedMediaId).isEqualTo(643)
	}
}
