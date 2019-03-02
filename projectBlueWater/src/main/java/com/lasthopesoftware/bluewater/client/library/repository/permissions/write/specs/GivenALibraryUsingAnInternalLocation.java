package com.lasthopesoftware.bluewater.client.library.repository.permissions.write.specs;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.LibraryStorageWritePermissionsRequirementsProvider;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 7/11/16.
 */
public class GivenALibraryUsingAnInternalLocation {

	public static class WhenCheckingIfStorageWritePermissionsAreRequired {

		private boolean isWritePermissionsRequired;

		@Before
		public void setUp() {
			final Library libraryRequiringExistingFiles = new Library();
			libraryRequiringExistingFiles.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL);

			final LibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider =
				new LibraryStorageWritePermissionsRequirementsProvider();

			isWritePermissionsRequired = libraryStorageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(libraryRequiringExistingFiles);
		}

		@Test
		public void thenTheReadPermissionsAreNotRequired() {
			Assert.assertFalse(isWritePermissionsRequired);
		}
	}
}
