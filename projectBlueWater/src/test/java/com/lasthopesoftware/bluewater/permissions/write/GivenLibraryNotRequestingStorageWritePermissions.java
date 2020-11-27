package com.lasthopesoftware.bluewater.permissions.write;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

import junit.framework.TestCase;

/**
 * Created by david on 7/10/16.
 */
public class GivenLibraryNotRequestingStorageWritePermissions {
	public static class AndOsGrantingPermissions extends TestCase {

		private boolean isPermissionGranted;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationWritePermissionsRequirementsProvider applicationWritePermissionsRequirementsProvider
					= new ApplicationWritePermissionsRequirementsProvider(library -> false, () -> true);

			this.isPermissionGranted = applicationWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(new Library());
		}

		public final void testThatPermissionIsNotRequired() {
			assertFalse(this.isPermissionGranted);
		}
	}

	public static class AndOsNotGrantingPermissions extends TestCase {

		private boolean isPermissionRequired;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationWritePermissionsRequirementsProvider applicationWritePermissionsRequirementsProvider
					= new ApplicationWritePermissionsRequirementsProvider(library -> false, () -> false);

			this.isPermissionRequired = applicationWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(new Library());
		}

		public final void testThatThePermissionsIsNotRequired() {
			assertFalse(this.isPermissionRequired);
		}
	}
}
