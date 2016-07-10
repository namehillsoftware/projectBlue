package com.lasthopesoftware.bluewater.permissions.read.specs;

import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider;

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
					= new ApplicationReadPermissionsRequirementsProvider(() -> true, () -> true);

			this.isPermissionRequired = applicationReadPermissionsRequirementsProvider.isReadPermissionsRequired();
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
					= new ApplicationReadPermissionsRequirementsProvider(() -> true, () -> false);

			this.isPermissionRequired = applicationReadPermissionsRequirementsProvider.isReadPermissionsRequired();
		}

		public final void testThatThePermissionIsRequired() {
			assertTrue(this.isPermissionRequired);
		}
	}
}
