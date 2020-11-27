package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 7/11/16.
 */

public class GivenALibraryUsingAnExternalLocation {

	public static class WhenCheckingIfStorageReadPermissionsAreRequired {

		private boolean isWritePermissionsRequired;

		@Before
		public void setUp() {
			final Library libraryRequiringExistingFiles = new Library();
			libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL);

			final LibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider =
					new LibraryStorageWritePermissionsRequirementsProvider();

			isWritePermissionsRequired = libraryStorageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(libraryRequiringExistingFiles);
		}

		@Test
		public void thenTheWritePermissionsAreRequired() {
			Assert.assertTrue(isWritePermissionsRequired);
		}
	}
}
