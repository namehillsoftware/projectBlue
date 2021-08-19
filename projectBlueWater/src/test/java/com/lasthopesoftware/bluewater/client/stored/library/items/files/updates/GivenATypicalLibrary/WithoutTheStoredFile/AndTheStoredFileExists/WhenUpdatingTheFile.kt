package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithoutTheStoredFile.AndTheStoredFileExists

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.ProvideMediaFileIds
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
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

		val filePropertiesProvider = FakeFilesPropertiesProvider()
		filePropertiesProvider.addFilePropertiesToCache(
			ServiceFile(4),
			mapOf(
				Pair(KnownFileProperties.ARTIST, "artist"),
				Pair(KnownFileProperties.ALBUM, "album"),
				Pair(KnownFileProperties.FILENAME, "my-filename.mp3")
			)
		)

		val fakeLibraryProvider = FakeLibraryProvider(
			Library()
				.setIsUsingExistingFiles(true)
				.setId(14)
				.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
		)
		val storedFileUpdater = StoredFileUpdater(
			ApplicationProvider.getApplicationContext(),
			mediaFileUriProvider,
			mediaFileIdProvider,
			StoredFileQuery(ApplicationProvider.getApplicationContext()),
			fakeLibraryProvider,
			filePropertiesProvider,
			SyncDirectoryLookup(
				fakeLibraryProvider,
				{ Promise(listOf(File("/my-public-drive"))) },
				{ Promise(emptyList()) }
			) { 0 })
		storedFile =
			storedFileUpdater.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4)).toFuture().get()
	}

	@Test
	fun thenTheFileIsInsertedIntoTheDatabase() {
		assertThat(
				StoredFileQuery(ApplicationProvider.getApplicationContext()).promiseStoredFile(
					LibraryId(14), ServiceFile(4)
				).toFuture().get()
		).isNotNull
	}

	@Test
	fun thenTheFileIsNotOwnedByTheLibrary() {
		assertThat(storedFile!!.isOwner).isFalse
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(storedFile!!.path).isEqualTo("/custom-root/a-file.mp3")
	}
}
