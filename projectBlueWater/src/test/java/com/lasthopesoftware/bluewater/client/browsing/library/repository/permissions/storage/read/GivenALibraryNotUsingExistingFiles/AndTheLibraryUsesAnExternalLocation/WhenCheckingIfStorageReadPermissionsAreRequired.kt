package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.GivenALibraryNotUsingExistingFiles.AndTheLibraryUsesAnExternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.LibraryStorageReadPermissionsRequirementsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {
	private var isReadPermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = Library()
		libraryRequiringExistingFiles.setIsUsingExistingFiles(true)
		libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL)
		val libraryStorageReadPermissionsRequirementsProvider = LibraryStorageReadPermissionsRequirementsProvider()
		isReadPermissionsRequired =
			libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(
				libraryRequiringExistingFiles
			)
	}

	@Test
	fun thenTheReadPermissionsAreRequired() {
		assertThat(isReadPermissionsRequired).isTrue
	}
}
