package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

public final class LibraryStorageWritePermissionsRequirementsProvider implements ILibraryStorageWritePermissionsRequirementsProvider {

	@Override
	public boolean isWritePermissionsRequiredForLibrary(@NonNull Library library) {
		return Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
