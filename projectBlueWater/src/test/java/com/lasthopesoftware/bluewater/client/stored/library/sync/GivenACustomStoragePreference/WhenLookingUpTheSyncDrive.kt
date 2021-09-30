package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenACustomStoragePreference

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.storage.directories.FakePrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.FakePublicDirectoryLookup
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class WhenLookingUpTheSyncDrive {
	@Test
	fun thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file!!.path).isEqualTo("/stoarage2/my-second-card")
	}

	companion object {
		private var file: File? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val publicDrives = FakePublicDirectoryLookup()
			publicDrives.addDirectory("", 1)
			publicDrives.addDirectory("", 2)
			publicDrives.addDirectory("", 3)
			publicDrives.addDirectory("/storage/0/my-big-sd-card", 4)
			val fakePrivateDirectoryLookup = FakePrivateDirectoryLookup()
			fakePrivateDirectoryLookup.addDirectory("fake-private-path", 3)
			fakePrivateDirectoryLookup.addDirectory("/fake-private-path", 5)
			val fakeLibraryProvider = FakeLibraryProvider(
				Library()
					.setId(14)
					.setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM)
					.setCustomSyncedFilesPath("/stoarage2/my-second-card")
			)
			val syncDirectoryLookup = SyncDirectoryLookup(
				fakeLibraryProvider,
				publicDrives,
				fakePrivateDirectoryLookup,
				{ f: File? -> 0 })
			file = FuturePromise(syncDirectoryLookup.promiseSyncDirectory(LibraryId(14))).get()
		}
	}
}
