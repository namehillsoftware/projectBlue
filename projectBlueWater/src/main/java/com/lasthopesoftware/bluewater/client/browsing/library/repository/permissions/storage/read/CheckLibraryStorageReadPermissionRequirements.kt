package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.read;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public interface ILibraryStorageReadPermissionsRequirementsProvider {
	boolean isReadPermissionsRequiredForLibrary(@NonNull Library library);
}
