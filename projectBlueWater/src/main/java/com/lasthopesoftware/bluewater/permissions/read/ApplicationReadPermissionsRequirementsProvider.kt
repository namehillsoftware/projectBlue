package com.lasthopesoftware.bluewater.permissions.read;

import android.content.Context;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

public final class ApplicationReadPermissionsRequirementsProvider implements IApplicationReadPermissionsRequirementsProvider {

	private final ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider;
	private final IStorageReadPermissionArbitratorForOs storageReadPermissionArbitratorForOs;

	public ApplicationReadPermissionsRequirementsProvider(@NonNull Context context) {
		this(new LibraryStorageReadPermissionsRequirementsProvider(), new ExternalStorageReadPermissionsArbitratorForOs(context));
	}

	public ApplicationReadPermissionsRequirementsProvider(@NonNull ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider,
	                                                      IStorageReadPermissionArbitratorForOs storageReadPermissionArbitratorForOs) {

		this.libraryStorageReadPermissionsRequirementsProvider = libraryStorageReadPermissionsRequirementsProvider;
		this.storageReadPermissionArbitratorForOs = storageReadPermissionArbitratorForOs;
	}

	@Override
	public boolean isReadPermissionsRequiredForLibrary(@NonNull Library library) {
		return libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library) && !storageReadPermissionArbitratorForOs.isReadPermissionGranted();
	}
}
