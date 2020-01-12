package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.specs;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.LibraryStorageWritePermissionsRequirementsProvider;

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
