package com.lasthopesoftware.bluewater.permissions.write;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public interface IApplicationWritePermissionsRequirementsProvider {
	boolean isWritePermissionsRequiredForLibrary(@NonNull Library library);
}
