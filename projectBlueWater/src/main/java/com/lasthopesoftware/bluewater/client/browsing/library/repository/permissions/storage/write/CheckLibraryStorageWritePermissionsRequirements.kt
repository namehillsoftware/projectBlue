package com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public interface ILibraryStorageWritePermissionsRequirementsProvider {
	boolean isWritePermissionsRequiredForLibrary(@NonNull Library library);
}
