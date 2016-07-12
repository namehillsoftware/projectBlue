package com.lasthopesoftware.bluewater.client.library.repository.permissions.read;

import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public interface ILibraryStorageReadPermissionsRequirementsProvider {
	boolean isReadPermissionsRequiredForLibrary(@NonNull Library library);
}
