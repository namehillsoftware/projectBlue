package com.lasthopesoftware.bluewater.permissions.write.specs;

import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider;

import junit.framework.TestCase;

/**
 * Created by david on 7/10/16.
 */
public class GivenLibraryRequestingStorageWritePermissions {
	public static class AndOsGrantingPermissions extends TestCase {

		private boolean isPermissionRequired;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationWritePermissionsRequirementsProvider applicationWritePermissionsRequirementsProvider
					= new ApplicationWritePermissionsRequirementsProvider(() -> true, () -> true);

			this.isPermissionRequired = applicationWritePermissionsRequirementsProvider.isWritePermissionsRequired();
		}

		public final void testThatPermissionIsNotRequired() {
			assertFalse(this.isPermissionRequired);
		}
	}

	public static class AndOsNotGrantingPermissions extends TestCase {

		private boolean isPermissionRequired;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationWritePermissionsRequirementsProvider applicationWritePermissionsRequirementsProvider
					= new ApplicationWritePermissionsRequirementsProvider(() -> true, () -> false);

			this.isPermissionRequired = applicationWritePermissionsRequirementsProvider.isWritePermissionsRequired();
		}

		public final void testThatPermissionIsRequired() {
			assertTrue(this.isPermissionRequired);
		}
	}
}
