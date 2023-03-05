package com.lasthopesoftware.bluewater.permissions.write

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.ILibraryStorageWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.permissions.storage.write.LibraryStorageWritePermissionsRequirementsProvider
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs

class ApplicationWritePermissionsRequirementsProvider(
    private val storageWritePermissionsRequirementsProvider: ILibraryStorageWritePermissionsRequirementsProvider,
    private val storageWritePermissionArbitratorForOs: IStorageWritePermissionArbitratorForOs
) : IApplicationWritePermissionsRequirementsProvider {
    constructor(context: Context?) : this(
        LibraryStorageWritePermissionsRequirementsProvider(),
        ExternalStorageWritePermissionsArbitratorForOs(context)
    )

	override fun isWritePermissionsRequiredForLibrary(library: Library): Boolean {
        return storageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(
            library
        ) && !storageWritePermissionArbitratorForOs.isWritePermissionGranted
    }
}
