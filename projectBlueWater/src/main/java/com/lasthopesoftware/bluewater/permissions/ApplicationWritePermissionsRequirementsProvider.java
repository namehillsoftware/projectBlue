package com.lasthopesoftware.bluewater.permissions;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.permissions.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.permissions.LibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.permissions.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.permissions.IStorageWritePermissionArbitratorForOs;

/**
 * Created by david on 7/3/16.
 */
public class ApplicationWritePermissionsRequirementsProvider implements IApplicationWritePermissionsRequirementsProvider {

	private final ILibraryStorageWritePermissionsRequirementsProvider storageWritePermissionsRequirementsProvider;
	private final IStorageWritePermissionArbitratorForOs storageWritePermissionArbitratorForOs;

	public ApplicationWritePermissionsRequirementsProvider(Context context, Library library) {
		this(new LibraryStorageWritePermissionsRequirementsProvider(library), new ExternalStorageWritePermissionsArbitratorForOs(context));
	}

	public ApplicationWritePermissionsRequirementsProvider(
			ILibraryStorageWritePermissionsRequirementsProvider storageWritePermissionsRequirementsProvider, IStorageWritePermissionArbitratorForOs storageWritePermissionArbitratorForOs) {

		this.storageWritePermissionsRequirementsProvider = storageWritePermissionsRequirementsProvider;
		this.storageWritePermissionArbitratorForOs = storageWritePermissionArbitratorForOs;
	}

	@Override
	public boolean isWritePermissionsRequired() {
		return storageWritePermissionsRequirementsProvider.isWritePermissionsRequired() && !storageWritePermissionArbitratorForOs.isWritePermissionGranted();
	}
}
