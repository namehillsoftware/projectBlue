package com.lasthopesoftware.bluewater.client.library.repository.permissions;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public class LibraryStorageWritePermissionsRequirementsProvider implements ILibraryStorageWritePermissionsRequirementsProvider {
	private Library library;

	public LibraryStorageWritePermissionsRequirementsProvider(Library library) {
		this.library = library;
	}

	@Override
	public boolean isWritePermissionsRequired() {
		return Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
