package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenAnExternalStoragePreference

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.storage.directories.FakePrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.FakePublicDirectoryLookup
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class WhenLookingUpTheSyncDrive {

	private val file by lazy {
		val publicDrives = FakePublicDirectoryLookup()
		publicDrives.addDirectory("", 1)
		publicDrives.addDirectory("", 2)
		publicDrives.addDirectory("", 3)
		publicDrives.addDirectory("/storage/0/my-big-sd-card", 4)
		val fakePrivateDirectoryLookup = FakePrivateDirectoryLookup()
		fakePrivateDirectoryLookup.addDirectory("fake-private-path", 3)
		fakePrivateDirectoryLookup.addDirectory("/fake-private-path", 5)
		val syncDirectoryLookup = SyncDirectoryLookup(
			FakeLibraryRepository(
				Library().setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL).setId(14)
			),
			publicDrives,
			fakePrivateDirectoryLookup,
			publicDrives
		)
		ExpiringFuturePromise(syncDirectoryLookup.promiseSyncDirectory(LibraryId(14))).get()
	}

	@Test
	fun `then the drive is the one with the most space`() {
		assertThat(file!!.path).isEqualTo("/storage/0/my-big-sd-card/14")
	}
}
