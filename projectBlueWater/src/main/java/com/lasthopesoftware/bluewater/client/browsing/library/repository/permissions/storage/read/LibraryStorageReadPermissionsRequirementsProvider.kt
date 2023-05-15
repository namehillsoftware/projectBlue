package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

public final class LibraryStorageReadPermissionsRequirementsProvider implements ILibraryStorageReadPermissionsRequirementsProvider {

	@Override
	public boolean isReadPermissionsRequiredForLibrary(@NonNull Library library) {
		return library.isUsingExistingFiles() || Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
