package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import java.io.IOException
import java.util.*

class MediaQueryCursorProvider(context: Context, cachedFilePropertiesProvider: CachedFilePropertiesProvider) : IMediaQueryCursorProvider, ImmediateResponse<Map<String, String>, Cursor?> {
	private val context: Context
	private val cachedFilePropertiesProvider: CachedFilePropertiesProvider

	init {
		requireNotNull(context) { "Context cannot be null" }
		this.context = context
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider
	}

	override fun getMediaQueryCursor(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Cursor?> {
		return cachedFilePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.then(this)
	}

	@Throws(Exception::class)
	override fun respond(fileProperties: Map<String, String>): Cursor? {
		val originalFilename = fileProperties[KnownFileProperties.FILENAME]
			?: throw IOException("The filename property was not retrieved. A connection needs to be re-established.")

		val filename = originalFilename.substring(originalFilename.lastIndexOf('\\') + 1, originalFilename.lastIndexOf('.'))

		val querySb = StringBuilder(mediaDataQuery)
		querySb.appendAnd()

		val params = ArrayList<String>(5)
		params.add(filename)
		querySb
			.appendPropertyFilter(params, MediaStore.Audio.Media.ARTIST, fileProperties[KnownFileProperties.ARTIST])
			.appendAnd()
			.appendPropertyFilter(params, MediaStore.Audio.Media.ALBUM, fileProperties[KnownFileProperties.ALBUM])
			.appendAnd()
			.appendPropertyFilter(params, MediaStore.Audio.Media.TITLE, fileProperties[KnownFileProperties.NAME])
			.appendAnd()
			.appendPropertyFilter(params, MediaStore.Audio.Media.TRACK, fileProperties[KnownFileProperties.TRACK])

		return context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaQueryProjection, querySb.toString(), params.toTypedArray(), null)
	}

	private fun StringBuilder.appendPropertyFilter(params: ArrayList<String>, key: String, value: String?): StringBuilder {
		this.append(' ').append(key).append(' ')
		if (value != null) {
			this.append(" = ? ")
			params.add(value)
		} else {
			this.append(" IS NULL ")
		}
		return this
	}

	private fun StringBuilder.appendAnd(): StringBuilder {
		return this.append(" AND ")
	}

	companion object {
		private const val mediaDataQuery = MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' "
		private val mediaQueryProjection = arrayOf(MediaStore.Audio.Media.DATA)
	}
}
