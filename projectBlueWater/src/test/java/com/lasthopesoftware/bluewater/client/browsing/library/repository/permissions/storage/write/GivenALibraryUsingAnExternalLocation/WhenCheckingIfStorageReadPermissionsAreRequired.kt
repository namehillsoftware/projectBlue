package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.GivenALibraryUsingAnExternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.LibraryStorageWritePermissionsRequirementsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {
	private var isWritePermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = Library()
		libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
		val libraryStorageWritePermissionsRequirementsProvider = LibraryStorageWritePermissionsRequirementsProvider()
		isWritePermissionsRequired =
			libraryStorageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(
				libraryRequiringExistingFiles
			)
	}

	@Test
	fun thenTheWritePermissionsAreRequired() {
		assertThat(isWritePermissionsRequired).isTrue
	}
}
