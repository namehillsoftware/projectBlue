package com.lasthopesoftware.bluewater.client.stored.library.items.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.resources.uri.IoCommon
import com.lasthopesoftware.resources.uri.toUri
import com.namehillsoftware.handoff.promises.Promise
import java.io.File
import java.net.URI

class StoredFileUriProvider(
	private val storedFileAccess: AccessStoredFiles,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions
) : ProvideFileUrisForLibrary {
    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> {
        return storedFileAccess
			.getStoredFile(libraryId, serviceFile)
            .then { storedFile: StoredFile? ->
				storedFile
					?.takeUnless { !it.isDownloadComplete || it.uri.isNullOrEmpty() }
					?.uri
					?.let(::URI)
					?.takeIf {
						when (it.scheme) {
							IoCommon.contentUriScheme -> !externalStorageReadPermissionsArbitrator.isReadPermissionGranted || !externalStorageReadPermissionsArbitrator.isReadMediaAudioPermissionGranted
							IoCommon.fileUriScheme -> File(it).exists()
							else -> true
						}
					}
					?.toUri()
            }
    }
}
