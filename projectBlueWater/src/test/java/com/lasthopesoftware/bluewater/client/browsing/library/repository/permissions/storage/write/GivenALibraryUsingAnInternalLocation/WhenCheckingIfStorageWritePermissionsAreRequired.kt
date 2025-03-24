package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.GivenALibraryUsingAnInternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.isWritePermissionsRequired
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageWritePermissionsAreRequired {
	private var isWritePermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = LibrarySettings(connectionSettings = StoredMediaCenterConnectionSettings(syncedFileLocation = SyncedFileLocation.INTERNAL))
		isWritePermissionsRequired = libraryRequiringExistingFiles.isWritePermissionsRequired
	}

	@Test
	fun thenTheReadPermissionsAreNotRequired() {
		assertThat(isWritePermissionsRequired).isFalse
	}
}
