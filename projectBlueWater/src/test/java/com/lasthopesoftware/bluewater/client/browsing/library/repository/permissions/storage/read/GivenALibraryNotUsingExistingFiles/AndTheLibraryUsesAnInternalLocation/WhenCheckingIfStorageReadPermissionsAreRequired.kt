package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.GivenALibraryNotUsingExistingFiles.AndTheLibraryUsesAnInternalLocation

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.LibraryStorageReadPermissionsRequirementsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingIfStorageReadPermissionsAreRequired {
	private val libraryStorageReadPermissionsRequirementsProvider by lazy {
		LibraryStorageReadPermissionsRequirementsProvider
	}

	private var isReadPermissionsRequired = false

	@BeforeAll
	fun act() {
		val libraryRequiringExistingFiles = Library()
		libraryRequiringExistingFiles.setIsUsingExistingFiles(false)
		libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL)
		isReadPermissionsRequired =
			libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(
				libraryRequiringExistingFiles
			)
	}

	@Test
	fun thenTheReadPermissionsAreNotRequired() {
		assertThat(isReadPermissionsRequired).isFalse
	}
}
