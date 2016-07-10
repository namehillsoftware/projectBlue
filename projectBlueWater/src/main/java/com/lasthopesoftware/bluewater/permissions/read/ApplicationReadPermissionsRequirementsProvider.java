package com.lasthopesoftware.bluewater.permissions.read;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.permissions.storage.read.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.permissions.storage.read.IStorageReadPermissionArbitratorForOs;

/**
 * Created by david on 7/3/16.
 */
public class ApplicationReadPermissionsRequirementsProvider implements IApplicationReadPermissionsRequirementsProvider {

	private final ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider;
	private final IStorageReadPermissionArbitratorForOs storageReadPermissionArbitratorForOs;

	public ApplicationReadPermissionsRequirementsProvider(Context context, Library library) {
		this(new LibraryStorageReadPermissionsRequirementsProvider(library), new ExternalStorageReadPermissionsArbitratorForOs(context));
	}

	public ApplicationReadPermissionsRequirementsProvider(ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider,
	                                                      IStorageReadPermissionArbitratorForOs storageReadPermissionArbitratorForOs) {

		this.libraryStorageReadPermissionsRequirementsProvider = libraryStorageReadPermissionsRequirementsProvider;
		this.storageReadPermissionArbitratorForOs = storageReadPermissionArbitratorForOs;
	}

	@Override
	public boolean isReadPermissionsRequired() {
		return libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequired() && !storageReadPermissionArbitratorForOs.isReadPermissionGranted();
	}
}
