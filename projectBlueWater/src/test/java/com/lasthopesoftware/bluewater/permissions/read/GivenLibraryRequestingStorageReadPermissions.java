package com.lasthopesoftware.bluewater.permissions.read;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

import junit.framework.TestCase;

/**
 * Created by david on 7/10/16.
 */
public class GivenLibraryRequestingStorageReadPermissions {
	public static class AndOsGrantingPermissions extends TestCase {

		private boolean isPermissionRequired;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationReadPermissionsRequirementsProvider applicationReadPermissionsRequirementsProvider
					= new ApplicationReadPermissionsRequirementsProvider(library -> true, () -> true);

			this.isPermissionRequired = applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(new Library());
		}

		public final void testThatThePermissionIsNotRequired() {
			assertFalse(this.isPermissionRequired);
		}
	}

	public static class AndOsNotGrantingPermissions extends TestCase {

		private boolean isPermissionRequired;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationReadPermissionsRequirementsProvider applicationReadPermissionsRequirementsProvider
					= new ApplicationReadPermissionsRequirementsProvider(library -> true, () -> false);

			this.isPermissionRequired = applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(new Library());
		}

		public final void testThatThePermissionIsRequired() {
			assertTrue(this.isPermissionRequired);
		}
	}
}
