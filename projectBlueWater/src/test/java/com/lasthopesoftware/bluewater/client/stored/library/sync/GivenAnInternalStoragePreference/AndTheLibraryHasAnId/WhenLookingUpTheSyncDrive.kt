package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenAnInternalStoragePreference.AndTheLibraryHasAnId

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
		val fakePrivateDirectoryLookup = FakePrivateDirectoryLookup()
		fakePrivateDirectoryLookup.addDirectory("", 1)
		fakePrivateDirectoryLookup.addDirectory("", 2)
		fakePrivateDirectoryLookup.addDirectory("", 3)
		fakePrivateDirectoryLookup.addDirectory("/storage/0/my-private-sd-card", 10)
		val publicDrives = FakePublicDirectoryLookup()
		publicDrives.addDirectory("fake-private-path", 12)
		publicDrives.addDirectory("/fake-private-path", 5)
		val fakeLibraryRepository = FakeLibraryRepository(
			Library()
				.setId(14)
				.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
		)
		val syncDirectoryLookup = SyncDirectoryLookup(
			fakeLibraryRepository,
			publicDrives,
			fakePrivateDirectoryLookup,
			fakePrivateDirectoryLookup
		)
		ExpiringFuturePromise(syncDirectoryLookup.promiseSyncDirectory(LibraryId(14))).get()
	}

	@Test
	fun `then the drive is the one with the most space`() {
		assertThat(file!!.path)
			.isEqualTo("/storage/0/my-private-sd-card/14")
	}
}
