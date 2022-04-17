package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenAnInternalStoragePreference.AndTheLibraryHasAnId

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.storage.directories.FakePrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.FakePublicDirectoryLookup
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class WhenLookingUpTheSyncDrive {
	@Test
	fun thenTheDriveIsTheOneWithTheMostSpace() {
		assertThat(file!!.path)
			.isEqualTo("/storage/0/my-private-sd-card/14")
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
			val fakeLibraryProvider = FakeLibraryProvider(
				Library()
					.setId(14)
					.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
			)
			val syncDirectoryLookup = SyncDirectoryLookup(
				fakeLibraryProvider,
				publicDrives,
				fakePrivateDirectoryLookup,
				fakePrivateDirectoryLookup
			)
			file = ExpiringFuturePromise(syncDirectoryLookup.promiseSyncDirectory(LibraryId(14))).get()
		}
	}
}
