package com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri

import android.net.Uri
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.IMediaQueryCursorProvider
import com.lasthopesoftware.bluewater.shared.IoCommon
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
		if (!externalStorageReadPermissionsArbitrator.isReadPermissionGranted) Promise.empty()
		else mediaQueryCursorProvider
			.getMediaQueryCursor(libraryId, serviceFile)
			.then { cursor ->
				cursor?.use {
					if (!cursor.moveToFirst()) return@then null
					val fileUriString =
						cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
					if (fileUriString == null || fileUriString.isEmpty()) return@then null

					// The file object will produce a properly escaped file URI, as opposed to what is stored in the DB
					val systemFile = File(
						fileUriString.replaceFirst(IoCommon.FileUriScheme + "://", "")
					)
					if (!systemFile.exists()) return@then null
					if (!isSilent) {

						val mediaId = try {
							cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey))
						} catch (ie: IllegalArgumentException) {
							logger.info("Illegal column name.", ie)
							null
						}

						sendApplicationMessages.sendMessage(
							MediaFileFound(
								libraryId,
								mediaId,
								serviceFile,
								systemFile
							)
						)
					}
					logger.info("Returning serviceFile URI from local disk.")
					Uri.fromFile(systemFile)
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
