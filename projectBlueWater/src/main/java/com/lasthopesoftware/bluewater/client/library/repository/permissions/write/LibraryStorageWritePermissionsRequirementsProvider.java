package com.lasthopesoftware.bluewater.client.library.repository.permissions.write;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.lazyj.Lazy;

public final class LibraryStorageWritePermissionsRequirementsProvider implements ILibraryStorageWritePermissionsRequirementsProvider {

	private static final Lazy<LibraryStorageWritePermissionsRequirementsProvider> lazyInstance = new Lazy<>(LibraryStorageWritePermissionsRequirementsProvider::new);

	private LibraryStorageWritePermissionsRequirementsProvider() {}

	@Override
	public boolean isWritePermissionsRequiredForLibrary(@NonNull Library library) {
		return Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}

	public static LibraryStorageWritePermissionsRequirementsProvider getInstance() {
		return lazyInstance.getObject();
	}
}
