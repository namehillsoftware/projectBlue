package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.File
import java.io.IOException

class MediaQueryCursorProvider
(
	private val context: Context,
	private val cachedFilePropertiesProvider: CachedFilePropertiesProvider
) : IMediaQueryCursorProvider, ImmediateResponse<Map<String, String>, Cursor?> {

	override fun getMediaQueryCursor(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Cursor?> {
		return cachedFilePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.then(this)
	}

	override fun respond(fileProperties: Map<String, String>): Cursor? {
		val originalFilename = fileProperties[KnownFileProperties.Filename]
			?: throw IOException("The filename property was not retrieved. A connection needs to be re-established.")

		val file = File(originalFilename)
		val filename = file.nameWithoutExtension

		val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
		} else {
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
		}

		return context.contentResolver.query(
			collectionUri,
			mediaQueryProjection,
			mediaDataQuery,
			arrayOf(filename),
			null)
	}

	companion object {
		private const val mediaDataQuery = MediaStore.Audio.Media.DISPLAY_NAME + " LIKE '%' || ? || '%' "
		private val mediaQueryProjection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
	}
}
