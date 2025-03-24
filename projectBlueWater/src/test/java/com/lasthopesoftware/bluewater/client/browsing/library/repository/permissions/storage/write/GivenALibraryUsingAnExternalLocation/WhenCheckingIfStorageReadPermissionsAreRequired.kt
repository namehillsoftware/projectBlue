package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.GivenALibraryUsingAnExternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.isWritePermissionsRequired
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {
	private var isWritePermissionsRequired = false

	@BeforeAll
	fun act() {
		val librarySettings = LibrarySettings(
			libraryId = LibraryId(945),
			connectionSettings = StoredMediaCenterConnectionSettings(
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
			),
		)
		isWritePermissionsRequired = librarySettings.isWritePermissionsRequired
	}

	@Test
	fun thenTheWritePermissionsAreRequired() {
		assertThat(isWritePermissionsRequired).isTrue
	}
}
