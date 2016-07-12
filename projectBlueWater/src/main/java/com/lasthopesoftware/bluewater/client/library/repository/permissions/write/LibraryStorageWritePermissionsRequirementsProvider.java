package com.lasthopesoftware.bluewater.client.library.repository.permissions.write;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public class LibraryStorageWritePermissionsRequirementsProvider implements ILibraryStorageWritePermissionsRequirementsProvider {
	private Library library;

	public LibraryStorageWritePermissionsRequirementsProvider(@NonNull Library library) {
		this.library = library;
	}

	@Override
	public boolean isWritePermissionsRequired() {
		return Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
