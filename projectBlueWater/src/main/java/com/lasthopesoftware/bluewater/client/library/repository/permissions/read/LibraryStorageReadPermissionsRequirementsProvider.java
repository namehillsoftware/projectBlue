package com.lasthopesoftware.bluewater.client.library.repository.permissions.read;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public class LibraryStorageReadPermissionsRequirementsProvider implements ILibraryStorageReadPermissionsRequirementsProvider {

		@Override
	public boolean isReadPermissionsRequiredForLibrary(@NonNull Library library) {
		return library.isUsingExistingFiles() || Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
