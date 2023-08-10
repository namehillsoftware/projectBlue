package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndSyncCanUseExternalFiles.AndTheStoredFileExists

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.ProvideMediaFileIds
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import java.io.File
import java.net.URI

class WhenUpdatingTheFile : AndroidContext() {

	companion object {
		private var storedFile: StoredFile? = null
		private val libraryId = LibraryId(14)
	}

	override fun before() {
		val mediaFileUriProvider = mockk<MediaFileUriProvider>()
		every { mediaFileUriProvider.promiseUri(libraryId, ServiceFile(4)) } returns Promise(Uri.fromFile(File("/custom-root/a-file.mp3")))

		val mediaFileIdProvider = mockk<ProvideMediaFileIds>()
		every { mediaFileIdProvider.getMediaId(libraryId, ServiceFile(4)) } returns Promise(12)

		val fakeLibraryRepository = FakeLibraryRepository(
			Library()
				.setIsUsingExistingFiles(true)
				.setId(14)
				.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
		)

		val storedFileUpdater = StoredFileUpdater(
			ApplicationProvider.getApplicationContext(),
			mediaFileUriProvider,
            StoredFileQuery(ApplicationProvider.getApplicationContext()),
			fakeLibraryRepository,
			mockk {
				every { promiseStoredFileUri(libraryId, ServiceFile(4)) } returns Promise(
					URI("file:/my-public-drive/busy/sweeten.mp3")
				)
			},
		)
		storedFile =
			storedFileUpdater.promiseStoredFileUpdate(libraryId, ServiceFile(4)).toExpiringFuture().get()
	}

	@Test
	fun thenTheFileIsInsertedIntoTheDatabase() {
		assertThat(
			StoredFileQuery(ApplicationProvider.getApplicationContext())
				.promiseStoredFile(LibraryId(14), ServiceFile(4))
				.toExpiringFuture()
				.get()
		).isNotNull
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
