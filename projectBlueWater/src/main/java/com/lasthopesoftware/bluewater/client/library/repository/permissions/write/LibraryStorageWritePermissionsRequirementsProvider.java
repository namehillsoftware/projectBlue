package com.lasthopesoftware.bluewater.client.library.repository.permissions.write;

import androidx.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

public final class LibraryStorageWritePermissionsRequirementsProvider implements ILibraryStorageWritePermissionsRequirementsProvider {

	@Override
	public boolean isWritePermissionsRequiredForLibrary(@NonNull Library library) {
		return Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}
}
