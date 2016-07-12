package com.lasthopesoftware.bluewater.client.library.repository.permissions.read.specs;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.LibraryStorageReadPermissionsRequirementsProvider;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 7/11/16.
 */

public class GivenALibraryUsingExistingFiles {

	public static class AndTheLibraryUsesAnInternalLocation {

		public static class WhenCheckingIfStorageReadPermissionsAreRequired {

			private boolean isReadPermissionsRequired;

			@Before
			public void setUp() {
				final Library libraryRequiringExistingFiles = mock(Library.class);
				when(libraryRequiringExistingFiles.isUsingExistingFiles()).thenReturn(true);
				when(libraryRequiringExistingFiles.getSyncedFileLocation()).thenReturn(Library.SyncedFileLocation.INTERNAL);
				final LibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider =
						new LibraryStorageReadPermissionsRequirementsProvider(libraryRequiringExistingFiles);

				isReadPermissionsRequired = libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequired();
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
				final Library libraryRequiringExistingFiles = mock(Library.class);
				when(libraryRequiringExistingFiles.isUsingExistingFiles()).thenReturn(true);
				when(libraryRequiringExistingFiles.getSyncedFileLocation()).thenReturn(Library.SyncedFileLocation.EXTERNAL);
				final LibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider =
						new LibraryStorageReadPermissionsRequirementsProvider(libraryRequiringExistingFiles);

				isReadPermissionsRequired = libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequired();
			}

			@Test
			public void thenTheReadPermissionsAreRequired() {
				Assert.assertTrue(isReadPermissionsRequired);
			}
		}
	}
}
