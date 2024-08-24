package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import android.content.ContentResolver
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.fileNameParts
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.io.IOException

class MediaQueryCursorProvider
(
	private val contentResolver: ContentResolver,
	private val cachedFilePropertiesProvider: ProvideLibraryFileProperties
) : ProvideMediaQueryCursor, PromisedResponse<Map<String, String>, Cursor?> {

	override fun getMediaQueryCursor(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Cursor?> =
		cachedFilePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.eventually(this)

	override fun promiseResponse(fileProperties: Map<String, String>): Promise<Cursor?> {
		val (_, baseFileName, _, postExtension) = fileProperties.fileNameParts
			?: return Promise(IOException("The filename property was not retrieved. A connection needs to be re-established."))

		var mediaCollectionFilter = pathFilter
		val selectionArgs = mutableListOf(baseFileName)
		if (postExtension.isNotEmpty()) {
			mediaCollectionFilter += " || ? "
			selectionArgs.add(postExtension)
		}
		if (Build.VERSION.SDK_INT >= 29) {
			mediaCollectionFilter += extendedMediaCollectionFilter
			selectionArgs.add(fileProperties[KnownFileProperties.Album] ?: "")
		}

		return QueuedPromise(
			MessageWriter {
				contentResolver.query(
					MediaCollections.ExternalAudio,
					mediaQueryProjection,
					mediaCollectionFilter,
					selectionArgs.toTypedArray(),
					null
				)
			}, ThreadPools.io)
	}

	companion object {
		private const val pathFilter = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE '%' || ? || '%' "

		private const val extendedMediaCollectionFilter = """ ${MediaStore.Audio.Media.IS_PENDING} = 0
			AND COALESCE(${MediaStore.Audio.AlbumColumns.ALBUM}, "") = ? """

		private val mediaQueryProjection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
	}
}
