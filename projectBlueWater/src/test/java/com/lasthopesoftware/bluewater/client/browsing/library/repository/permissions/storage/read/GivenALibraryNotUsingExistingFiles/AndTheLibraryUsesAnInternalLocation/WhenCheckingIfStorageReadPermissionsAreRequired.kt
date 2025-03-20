package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.GivenALibraryNotUsingExistingFiles.AndTheLibraryUsesAnInternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isReadPermissionsRequiredForLibrary
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {

	private var isReadPermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = Library(
			isUsingExistingFiles = false,
			connectionSettings = Json.encodeToString(
				StoredMediaCenterConnectionSettings(
					syncedFileLocation = SyncedFileLocation.INTERNAL,
				)
			),
		)
		isReadPermissionsRequired = libraryRequiringExistingFiles.isReadPermissionsRequiredForLibrary
	}

	@Test
	fun thenTheReadPermissionsAreNotRequired() {
		assertThat(isReadPermissionsRequired).isFalse
	}
}
