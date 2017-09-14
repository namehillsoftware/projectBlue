package com.lasthopesoftware.bluewater.client.library.repository.permissions.read;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.lazyj.Lazy;

public final class LibraryStorageReadPermissionsRequirementsProvider implements ILibraryStorageReadPermissionsRequirementsProvider {

	private static final Lazy<LibraryStorageReadPermissionsRequirementsProvider> lazyInstance = new Lazy<>(LibraryStorageReadPermissionsRequirementsProvider::new);

	private LibraryStorageReadPermissionsRequirementsProvider() {}

	@Override
	public boolean isReadPermissionsRequiredForLibrary(@NonNull Library library) {
		return library.isUsingExistingFiles() || Library.SyncedFileLocation.ExternalDiskAccessSyncLocations.contains(library.getSyncedFileLocation());
	}

	public static LibraryStorageReadPermissionsRequirementsProvider getInstance() {
		return lazyInstance.getObject();
	}
}
