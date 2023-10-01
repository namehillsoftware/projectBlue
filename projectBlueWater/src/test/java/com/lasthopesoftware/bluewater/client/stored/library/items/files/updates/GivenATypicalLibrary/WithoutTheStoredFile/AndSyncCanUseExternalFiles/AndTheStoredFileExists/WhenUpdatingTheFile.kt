package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndSyncCanUseExternalFiles.AndTheStoredFileExists

import android.net.Uri
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.FakeStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.net.URI

class WhenUpdatingTheFile : AndroidContext() {

	companion object {
		private var storedFile: StoredFile? = null
		private val libraryId = LibraryId(14)
		private val affectedSystems by lazy { FakeStoredFileAccess() }
	}

	override fun before() {
		val mediaFileUriProvider = mockk<MediaFileUriProvider> {
			every { promiseUri(libraryId, ServiceFile(4)) } returns Uri.fromFile(File("/custom-root/a-file.mp3")).toPromise()
		}

		val fakeLibraryRepository = FakeLibraryRepository(
			Library(
				isUsingExistingFiles = true,
				id = 14,
				syncedFileLocation = Library.SyncedFileLocation.EXTERNAL
			)
		)

		val storedFileUpdater = StoredFileUpdater(
			affectedSystems,
			mediaFileUriProvider,
            fakeLibraryRepository,
			mockk {
				every { promiseStoredFileUri(libraryId, ServiceFile(4)) } returns Promise(
					URI("file:/my-public-drive/busy/sweeten.mp3")
				)
			},
			mockk(),
		)
		storedFile =
			storedFileUpdater.promiseStoredFileUpdate(libraryId, ServiceFile(4)).toExpiringFuture().get()
	}

	@Test
	fun thenTheFileIsInsertedIntoTheDatabase() {
		assertThat(affectedSystems.storedFiles.values).allMatch { sf -> sf.libraryId == 14 && sf.serviceId == 4 }
	}

	@Test
	fun thenTheFileIsNotOwnedByTheLibrary() {
		assertThat(storedFile?.isOwner).isFalse
	}

	@Test
	fun `then the file path is correct`() {
		assertThat(storedFile?.uri).isEqualTo("file:///custom-root/a-file.mp3")
	}
}
