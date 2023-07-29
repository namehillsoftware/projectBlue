package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.GivenALibraryUsingAnInternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.isWritePermissionsRequiredForLibrary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageWritePermissionsAreRequired {
	private var isWritePermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = Library()
		libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
		isWritePermissionsRequired = libraryRequiringExistingFiles.isWritePermissionsRequiredForLibrary
	}

	@Test
	fun thenTheReadPermissionsAreNotRequired() {
		assertThat(isWritePermissionsRequired).isFalse
	}
}
