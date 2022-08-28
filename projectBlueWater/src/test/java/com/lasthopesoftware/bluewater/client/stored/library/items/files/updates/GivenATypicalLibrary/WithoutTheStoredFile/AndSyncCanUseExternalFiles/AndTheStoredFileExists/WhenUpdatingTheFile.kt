package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndSyncCanUseExternalFiles.AndTheStoredFileExists

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.ProvideMediaFileIds
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GetStoredFilePaths
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import java.io.File

class WhenUpdatingTheFile : AndroidContext() {

	companion object {
		private var storedFile: StoredFile? = null
	}

	override fun before() {
		val mediaFileUriProvider = mockk<MediaFileUriProvider>()
		every { mediaFileUriProvider.promiseFileUri(ServiceFile(4)) } returns Promise(Uri.fromFile(File("/custom-root/a-file.mp3")))

		val mediaFileIdProvider = mockk<ProvideMediaFileIds>()
		every { mediaFileIdProvider.getMediaId(LibraryId(14), ServiceFile(4)) } returns Promise(12)

		val fakeLibraryProvider = FakeLibraryProvider(
			Library()
				.setIsUsingExistingFiles(true)
				.setId(14)
				.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
		)

		val lookupStoredFilePaths = mockk<GetStoredFilePaths>()
		every { lookupStoredFilePaths.promiseStoredFilePath(LibraryId(14), ServiceFile(4)) } returns Promise("/my-public-drive/busy/sweeten.mp3")

		val storedFileUpdater = StoredFileUpdater(
			ApplicationProvider.getApplicationContext(),
			mediaFileUriProvider,
			mediaFileIdProvider,
			StoredFileQuery(ApplicationProvider.getApplicationContext()),
			fakeLibraryProvider,
			lookupStoredFilePaths
		)
		storedFile =
			storedFileUpdater.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4)).toExpiringFuture().get()
	}

	@Test
	fun thenTheFileIsInsertedIntoTheDatabase() {
		assertThat(
				StoredFileQuery(ApplicationProvider.getApplicationContext()).promiseStoredFile(
					LibraryId(14), ServiceFile(4)
				).toExpiringFuture().get()
		).isNotNull
	}

	@Test
	fun thenTheFileIsNotOwnedByTheLibrary() {
		assertThat(storedFile?.isOwner).isFalse
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(storedFile?.path).isEqualTo("/custom-root/a-file.mp3")
	}
}
