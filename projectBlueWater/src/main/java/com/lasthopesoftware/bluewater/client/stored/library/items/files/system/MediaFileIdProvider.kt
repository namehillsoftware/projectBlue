package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import android.database.Cursor
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class MediaFileIdProvider(
	private val mediaQueryCursorProvider: IMediaQueryCursorProvider,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions
) : ImmediateResponse<Cursor?, Int>, ProvideMediaFileIds {
	override fun getMediaId(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Int> =
		if (externalStorageReadPermissionsArbitrator.run { !isReadPermissionGranted || !isReadMediaAudioPermissionGranted }) Promise(-1)
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
		private val logger by lazyLogger<MediaFileIdProvider>()
		private const val audioIdKey = MediaStore.Audio.Media._ID
	}
}
