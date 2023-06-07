package com.lasthopesoftware.bluewater.client.stored.library.items.files.uri

import android.net.Uri
import android.os.Environment
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class StoredFileUriProvider(
	private val storedFileAccess: AccessStoredFiles,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions
) : ProvideFileUrisForLibrary {
    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> {
        return storedFileAccess
			.getStoredFile(libraryId, serviceFile)
            .then { storedFile: StoredFile? ->
                if (storedFile == null || !storedFile.isDownloadComplete || storedFile.path == null || storedFile.path.isEmpty()) return@then null
                val systemFile = File(storedFile.path)
                if (systemFile.absolutePath.contains(Environment.getExternalStorageDirectory().absolutePath) && !externalStorageReadPermissionsArbitrator.isReadPermissionGranted) return@then null
                if (systemFile.exists()) return@then Uri.fromFile(systemFile)
                null
            }
    }
}
