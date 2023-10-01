package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.GivenALibraryUsingExistingFiles.AndTheLibraryUsesAnExternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isReadPermissionsRequiredForLibrary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {
	private var isReadPermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = Library(
			isUsingExistingFiles = true,
			syncedFileLocation = Library.SyncedFileLocation.EXTERNAL
		)
		isReadPermissionsRequired = libraryRequiringExistingFiles.isReadPermissionsRequiredForLibrary
	}

	@Test
	fun thenTheReadPermissionsAreRequired() {
		assertThat(isReadPermissionsRequired).isTrue
	}
}
