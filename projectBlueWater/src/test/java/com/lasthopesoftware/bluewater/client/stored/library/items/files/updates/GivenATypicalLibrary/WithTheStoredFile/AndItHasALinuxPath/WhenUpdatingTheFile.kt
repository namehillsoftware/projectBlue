package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItHasALinuxPath

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilesPropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WhenUpdatingTheFile {

	companion object {
		private val storedFile by lazy {
			val mediaFileUriProvider = mockk<MediaFileUriProvider>()
			every { mediaFileUriProvider.promiseFileUri(any()) } returns Promise.empty()
			val mediaFileIdProvider = mockk<MediaFileIdProvider>()
			every { mediaFileIdProvider.getMediaId(any(), any()) } returns Promise.empty()
			val filePropertiesProvider = FakeFilesPropertiesProvider()
			filePropertiesProvider.addFilePropertiesToCache(
				ServiceFile(4),
				mapOf(
					Pair(KnownFileProperties.ARTIST, "artist"),
					Pair(KnownFileProperties.ALBUM, "album"),
					Pair(KnownFileProperties.FILENAME, "/my/linux-volume/a_filename.mp3"),
				))
			val fakeLibraryProvider = FakeLibraryProvider(
				Library().setId(14).setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
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
					{ Promise<Collection<File>>(listOf(File("/my-public-drive"))) },
					{ Promise<Collection<File>>(listOf(File("/private-drive"))) },
					{ 0 }
				)
			)

			storedFileUpdater
				.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4))
				.toFuture()
				.get()!!
		}
	}

	@Test
	fun thenTheFileIsOwnedByTheLibrary() {
		assertThat(storedFile.isOwner).isTrue
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(storedFile.path)
			.isEqualTo("/private-drive/14/artist/album/a_filename.mp3")
	}
}