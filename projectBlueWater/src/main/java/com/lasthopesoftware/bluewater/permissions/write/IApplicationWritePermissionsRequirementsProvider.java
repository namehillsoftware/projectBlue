package com.lasthopesoftware.bluewater.permissions.write;

import android.support.annotation.NonNull;
import com.lasthopesoftware.bluewater.client.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public interface IApplicationWritePermissionsRequirementsProvider {
	boolean isWritePermissionsRequiredForLibrary(@NonNull Library library);
}
