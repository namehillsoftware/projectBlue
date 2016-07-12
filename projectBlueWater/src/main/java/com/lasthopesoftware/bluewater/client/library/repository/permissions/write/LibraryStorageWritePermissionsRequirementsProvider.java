package com.lasthopesoftware.bluewater.client.library.repository.permissions.write;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public class LibraryStorageWritePermissionsRequirementsProvider implements ILibraryStorageWritePermissionsRequirementsProvider {
	@Override
	public boolean isWritePermissionsRequiredForLibrary(@NonNull Library library) {
		return Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
