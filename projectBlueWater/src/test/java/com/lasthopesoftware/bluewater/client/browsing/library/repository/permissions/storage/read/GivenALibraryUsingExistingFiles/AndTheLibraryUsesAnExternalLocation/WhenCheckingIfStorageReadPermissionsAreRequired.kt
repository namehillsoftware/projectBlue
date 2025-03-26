package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.GivenALibraryUsingExistingFiles.AndTheLibraryUsesAnExternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.isReadPermissionsRequired
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {
	private var isReadPermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = LibrarySettings(
			isUsingExistingFiles = true,
			syncedFileLocation = SyncedFileLocation.EXTERNAL,
			connectionSettings = StoredMediaCenterConnectionSettings(),
		)
		isReadPermissionsRequired = libraryRequiringExistingFiles.isReadPermissionsRequired
	}

	@Test
	fun thenTheReadPermissionsAreRequired() {
		assertThat(isReadPermissionsRequired).isTrue
	}
}
