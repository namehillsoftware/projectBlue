package com.lasthopesoftware.bluewater.client.library.repository.permissions.read.specs;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.LibraryStorageReadPermissionsRequirementsProvider;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 7/11/16.
 */

public class GivenALibraryUsingExistingFiles {

	public static class AndTheLibraryUsesAnInternalLocation {

		public static class WhenCheckingIfStorageReadPermissionsAreRequired {

			private boolean isReadPermissionsRequired;

			@Before
			public void setUp() {
				final Library libraryRequiringExistingFiles = new Library();
				libraryRequiringExistingFiles.setIsUsingExistingFiles(true);
				libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL);

				final LibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider =
					new LibraryStorageReadPermissionsRequirementsProvider();

				isReadPermissionsRequired = libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(libraryRequiringExistingFiles);
			}

			@Test
			public void thenTheReadPermissionsAreRequired() {
				Assert.assertTrue(isReadPermissionsRequired);
			}
		}
	}

	public static class AndTheLibraryUsesAnExternalLocation {

		public static class WhenCheckingIfStorageReadPermissionsAreRequired {

			private boolean isReadPermissionsRequired;

			@Before
			public void setUp() {
				final Library libraryRequiringExistingFiles = new Library();
				libraryRequiringExistingFiles.setIsUsingExistingFiles(true);
				libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL);

				final LibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider =
						new LibraryStorageReadPermissionsRequirementsProvider();

				isReadPermissionsRequired = libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(libraryRequiringExistingFiles);
			}

			@Test
			public void thenTheReadPermissionsAreRequired() {
				Assert.assertTrue(isReadPermissionsRequired);
			}
		}
	}
}
