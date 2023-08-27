package com.lasthopesoftware.bluewater.client.stored.library.items.files.uri

import android.net.Uri
import androidx.core.net.toFile
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.resources.uri.IoCommon
import com.namehillsoftware.handoff.promises.Promise

class StoredFileUriProvider(
	private val storedFileAccess: AccessStoredFiles,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions
) : ProvideFileUrisForLibrary {
    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> {
        return storedFileAccess
			.promiseStoredFile(libraryId, serviceFile)
            .then { storedFile ->
				storedFile
					?.takeUnless { !it.isDownloadComplete || it.uri.isNullOrEmpty() }
					?.uri
					?.let(Uri::parse)
					?.takeIf {
						when (it.scheme) {
							IoCommon.contentUriScheme -> !externalStorageReadPermissionsArbitrator.isReadPermissionGranted || !externalStorageReadPermissionsArbitrator.isReadMediaAudioPermissionGranted
							IoCommon.fileUriScheme -> it.toFile().exists()
							else -> false
						}
					}
            }
    }
}
