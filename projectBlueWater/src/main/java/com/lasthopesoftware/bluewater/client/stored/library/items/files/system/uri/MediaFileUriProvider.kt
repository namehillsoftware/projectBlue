package com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.IMediaQueryCursorProvider
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class MediaFileUriProvider(
	private val mediaQueryCursorProvider: IMediaQueryCursorProvider,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	private val isSilent: Boolean,
	private val sendApplicationMessages: SendApplicationMessages
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
					val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
					} else {
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
					}

//					if (!isSilent) {
//
//						val mediaId = try {
//							it.getInt(cursor.getColumnIndexOrThrow(audioIdKey))
//						} catch (ie: IllegalArgumentException) {
//							logger.info("Illegal column name.", ie)
//							null
//						}
//
//						sendApplicationMessages.sendMessage(
//							MediaFileFound(
//								libraryId,
//								mediaId,
//								serviceFile,
//								systemFile
//							)
//						)
//					}

					val uri = ContentUris.withAppendedId(collectionUri, fileId)
					logger.info("Returning media file URI {} from local disk.", uri)
					uri
				}
			}

	class MediaFileFound(
		val libraryId: LibraryId,
		val mediaId: Int?,
		val serviceFile: ServiceFile,
		val systemFile: File,
	) : ApplicationMessage

    companion object {
		private const val audioIdKey = MediaStore.Audio.Media._ID
        private val logger by lazyLogger<MediaFileUriProvider>()
    }
}
