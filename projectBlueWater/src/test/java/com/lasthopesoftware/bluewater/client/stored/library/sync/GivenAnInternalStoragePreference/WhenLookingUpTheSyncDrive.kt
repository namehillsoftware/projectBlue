package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenAnInternalStoragePreference

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
		assertThat(file!!.path).isEqualTo("/storage/0/my-private-sd-card/1")
	}

	companion object {
		private var file: File? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakePrivateDirectoryLookup = FakePrivateDirectoryLookup()
			fakePrivateDirectoryLookup.addDirectory("", 1)
			fakePrivateDirectoryLookup.addDirectory("", 2)
			fakePrivateDirectoryLookup.addDirectory("", 3)
			fakePrivateDirectoryLookup.addDirectory("/storage/0/my-private-sd-card", 10)
			val publicDrives = FakePublicDirectoryLookup()
			publicDrives.addDirectory("fake-private-path", 12)
			publicDrives.addDirectory("/fake-private-path", 5)
			val syncDirectoryLookup = SyncDirectoryLookup(
				FakeLibraryProvider(
					Library()
						.setId(1)
						.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
				),
				publicDrives,
				fakePrivateDirectoryLookup,
				fakePrivateDirectoryLookup
			)
			file = FuturePromise(syncDirectoryLookup.promiseSyncDirectory(LibraryId(1))).get()
		}
	}
}