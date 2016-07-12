package com.lasthopesoftware.bluewater.client.library.repository.permissions.read;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public class LibraryStorageReadPermissionsRequirementsProvider implements ILibraryStorageReadPermissionsRequirementsProvider {

	private final Library library;

	public LibraryStorageReadPermissionsRequirementsProvider(@NonNull Library library) {
		this.library = library;
	}

	@Override
	public boolean isReadPermissionsRequired() {
		return library.isUsingExistingFiles() || Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
