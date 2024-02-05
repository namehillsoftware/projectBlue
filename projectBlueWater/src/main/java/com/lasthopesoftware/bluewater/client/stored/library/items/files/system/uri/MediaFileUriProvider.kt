package com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.ProvideMediaQueryCursor
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.uri.MediaCollections
import com.lasthopesoftware.resources.uri.resourceExists
import com.namehillsoftware.handoff.promises.Promise

class MediaFileUriProvider(
    private val mediaQueryCursorProvider: ProvideMediaQueryCursor,
    private val externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	private val contentResolver: ContentResolver,
) : ProvideFileUrisForLibrary {

    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		if (externalStorageReadPermissionsArbitrator.run { !isReadPermissionGranted && !isReadMediaAudioPermissionGranted }) Promise.empty()
		else mediaQueryCursorProvider
			.getMediaQueryCursor(libraryId, serviceFile)
			.then { cursor ->
				cursor?.use {
					if (!it.moveToFirst()) return@then null
					val fileId = it.getLongOrNull(it.getColumnIndexOrThrow(audioIdKey)) ?: return@then null

					// The file object will produce a properly escaped file URI, as opposed to what is stored in the DB
					val collectionUri = MediaCollections.ExternalAudio

					val uri = ContentUris.withAppendedId(collectionUri, fileId)
					uri.takeIf(contentResolver::resourceExists)?.also {
						logger.info("Returning media file URI {} from local disk.", uri)
					}
				}
			}

	companion object {
		private const val audioIdKey = MediaStore.Audio.Media._ID
        private val logger by lazyLogger<MediaFileUriProvider>()
    }
}
