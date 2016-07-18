package com.lasthopesoftware.bluewater.permissions.read;

import android.content.Context;
import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

/**
 * Created by david on 7/3/16.
 */
public class ApplicationReadPermissionsRequirementsProvider implements IApplicationReadPermissionsRequirementsProvider {

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
