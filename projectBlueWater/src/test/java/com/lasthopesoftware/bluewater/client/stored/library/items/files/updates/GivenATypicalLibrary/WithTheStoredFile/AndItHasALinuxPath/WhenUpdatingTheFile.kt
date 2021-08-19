package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.GivenATypicalLibrary.WithTheStoredFile.AndItHasALinuxPath

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
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.storage.GetFreeSpace
import com.lasthopesoftware.storage.directories.GetPrivateDirectories
import com.lasthopesoftware.storage.directories.GetPublicDirectories
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
				object : GetPublicDirectories {
					override fun promisePublicDrives(): Promise<Collection<File>> =
						Promise<Collection<File>>(listOf(File("/my-public-drive")))
				},
				object : GetPrivateDirectories {
					override fun promisePrivateDrives(): Promise<Collection<File>> =
						Promise<Collection<File>>(listOf(File("/private-drive")))
				},
				object : GetFreeSpace {
					override fun getFreeSpace(file: File): Long = 0
				})
		)

		storedFile =
			storedFileUpdater.promiseStoredFileUpdate(LibraryId(14), ServiceFile(4)).toFuture().get()
	}

	@Test
	fun thenTheFileIsOwnedByTheLibrary() {
		assertThat(storedFile!!.isOwner).isTrue
	}

	@Test
	fun thenTheFilePathIsCorrect() {
		assertThat(storedFile!!.path)
			.isEqualTo("/private-drive/14/artist/album/a_filename.mp3")
	}
}
