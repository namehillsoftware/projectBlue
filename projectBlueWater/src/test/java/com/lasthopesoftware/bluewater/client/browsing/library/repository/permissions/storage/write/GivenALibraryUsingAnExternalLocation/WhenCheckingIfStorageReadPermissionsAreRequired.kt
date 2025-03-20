package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.GivenALibraryUsingAnExternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isWritePermissionsRequiredForLibrary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {
	private var isWritePermissionsRequired = false

	@BeforeAll
	fun act() {
		val connectionSettings = StoredMediaCenterConnectionSettings(
			syncedFileLocation = SyncedFileLocation.EXTERNAL
		)
		isWritePermissionsRequired = connectionSettings.isWritePermissionsRequiredForLibrary
	}

	@Test
	fun thenTheWritePermissionsAreRequired() {
		assertThat(isWritePermissionsRequired).isTrue
	}
}
