package com.lasthopesoftware.bluewater.client.library.repository.permissions.read;

import androidx.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

public final class LibraryStorageReadPermissionsRequirementsProvider implements ILibraryStorageReadPermissionsRequirementsProvider {

	@Override
	public boolean isReadPermissionsRequiredForLibrary(@NonNull Library library) {
		return library.isUsingExistingFiles() || Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
