package com.lasthopesoftware.bluewater.client.stored.library.items.files.uri

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toFile
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.resources.uri.IoCommon
import com.namehillsoftware.handoff.promises.Promise
import java.io.FileNotFoundException

class StoredFileUriProvider(
	private val storedFileAccess: AccessStoredFiles,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	private val contentResolver: ContentResolver,
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
							IoCommon.contentUriScheme -> isContentReadable(it)
							IoCommon.fileUriScheme -> it.toFile().exists()
							else -> false
						}
					}
            }
    }

	private fun isContentReadable(uri: Uri): Boolean {
		val isReadPermissionGranted = (!externalStorageReadPermissionsArbitrator.isReadPermissionGranted
			|| !externalStorageReadPermissionsArbitrator.isReadMediaAudioPermissionGranted)

		if (!isReadPermissionGranted) return false

		return try {
			contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
		} catch(e: FileNotFoundException) {
			false
		}
	}
}
