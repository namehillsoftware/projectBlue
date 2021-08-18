package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import android.database.Cursor
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.slf4j.LoggerFactory

class MediaFileIdProvider(
    private val mediaQueryCursorProvider: IMediaQueryCursorProvider,
    private val externalStorageReadPermissionsArbitrator: IStorageReadPermissionArbitratorForOs
) : ImmediateResponse<Cursor?, Int> {
    fun getMediaId(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Int> =
        if (!externalStorageReadPermissionsArbitrator.isReadPermissionGranted) Promise(-1)
		else mediaQueryCursorProvider
            .getMediaQueryCursor(libraryId, serviceFile)
            .then(this)

    override fun respond(cursor: Cursor?): Int {
        if (cursor == null) return -1
        try {
            if (cursor.moveToFirst()) return cursor.getInt(cursor.getColumnIndexOrThrow(audioIdKey))
        } catch (ie: IllegalArgumentException) {
            logger.info("Illegal column name.", ie)
        } finally {
            cursor.close()
        }
        return -1
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            MediaFileIdProvider::class.java
        )
        private val audioIdKey = MediaStore.Audio.keyFor("audio_id")
    }
}
