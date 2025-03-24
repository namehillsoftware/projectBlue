package com.lasthopesoftware.bluewater.client.stored.library.sync.GivenAnInternalStoragePreference.AndTheLibraryHasAnId

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.storage.directories.FakePrivateDirectoryLookup
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class WhenLookingUpTheSyncDrive {
	private val file by lazy {
		val fakePrivateDirectoryLookup = FakePrivateDirectoryLookup()
		fakePrivateDirectoryLookup.addDirectory("", 1)
		fakePrivateDirectoryLookup.addDirectory("", 2)
		fakePrivateDirectoryLookup.addDirectory("", 3)
		fakePrivateDirectoryLookup.addDirectory("/storage/0/my-private-sd-card", 10)

		val syncDirectoryLookup = SyncDirectoryLookup(
			mockk {
				every { promiseLibrarySettings(LibraryId(14)) } returns Promise(
					LibrarySettings(
						libraryId = LibraryId(14),
						connectionSettings = StoredMediaCenterConnectionSettings(
							syncedFileLocation = SyncedFileLocation.INTERNAL,
						)
					)
				)
			},
            fakePrivateDirectoryLookup,
			fakePrivateDirectoryLookup
		)
		syncDirectoryLookup.promiseSyncDirectory(LibraryId(14)).toExpiringFuture().get()
	}

	@Test
	fun `then the drive is the one with the most space`() {
		assertThat(file!!.path)
			.isEqualTo("/storage/0/my-private-sd-card/14")
	}
}
