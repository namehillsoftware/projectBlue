package com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.IMediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.shared.IoCommon
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder.Companion.buildMagicPropertyName
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.File

class MediaFileUriProvider
(
    private val context: Context,
    private val mediaQueryCursorProvider: IMediaQueryCursorProvider,
    private val externalStorageReadPermissionsArbitrator: IStorageReadPermissionArbitratorForOs,
    private val libraryIdentifierProvider: ProvideSelectedLibraryId,
    private val isSilent: Boolean
) : IFileUriProvider, ProvideFileUrisForLibrary {
    override fun promiseFileUri(serviceFile: ServiceFile): Promise<Uri> =
		libraryIdentifierProvider.selectedLibraryId.eventually {
			it?.let { promiseUri(it, serviceFile) }.keepPromise()
		}

    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri> =
		if (!externalStorageReadPermissionsArbitrator.isReadPermissionGranted) Promise.empty() else mediaQueryCursorProvider
			.getMediaQueryCursor(libraryId, serviceFile)
			.then { cursor ->
				cursor?.use {
					if (!cursor.moveToFirst()) return@then null
					val fileUriString =
						cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
					if (fileUriString == null || fileUriString.isEmpty()) return@then null

					// The serviceFile object will produce a properly escaped ServiceFile URI, as opposed to what is stored in the DB
					val systemFile = File(
						fileUriString.replaceFirst(IoCommon.FileUriScheme + "://", "")
					)
					if (!systemFile.exists()) return@then null
					if (!isSilent) {
						val broadcastIntent = Intent(mediaFileFoundEvent)
						broadcastIntent.putExtra(mediaFileFoundPath, systemFile.path)
						try {
							broadcastIntent.putExtra(
								mediaFileFoundMediaId,
								cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey))
							)
						} catch (ie: IllegalArgumentException) {
							logger.info("Illegal column name.", ie)
						}
						broadcastIntent.putExtra(mediaFileFoundFileKey, serviceFile.key)
						broadcastIntent.putExtra(mediaFileFoundLibraryId, libraryId.id)
						LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent)
					}
					logger.info("Returning serviceFile URI from local disk.")
					Uri.fromFile(systemFile)
				}
			}

    companion object {
        val mediaFileFoundEvent = buildMagicPropertyName(
            MediaFileUriProvider::class.java, "mediaFileFoundEvent"
        )
        val mediaFileFoundMediaId = buildMagicPropertyName(
            MediaFileUriProvider::class.java, "mediaFileFoundMediaId"
        )
        val mediaFileFoundFileKey = buildMagicPropertyName(
            MediaFileUriProvider::class.java, "mediaFileFoundFileKey"
        )
        val mediaFileFoundPath = buildMagicPropertyName(
            MediaFileUriProvider::class.java, "mediaFileFoundPath"
        )
        val mediaFileFoundLibraryId = buildMagicPropertyName(
            MediaFileUriProvider::class.java, "mediaFileFoundLibraryId"
        )
        private val audioIdKey = MediaStore.Audio.keyFor("audio_id")
        private val logger = LoggerFactory.getLogger(
            MediaFileUriProvider::class.java
        )
    }
}
