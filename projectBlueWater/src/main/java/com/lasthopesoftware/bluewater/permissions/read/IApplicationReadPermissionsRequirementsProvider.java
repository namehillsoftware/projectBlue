package com.lasthopesoftware.bluewater.permissions.read;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public interface IApplicationReadPermissionsRequirementsProvider {
	boolean isReadPermissionsRequiredForLibrary(@NonNull Library library);
}
