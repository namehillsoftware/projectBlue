package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import androidx.core.net.toUri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.AccessStoredFiles
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.apache.commons.io.FilenameUtils
import java.io.IOException

class MediaQueryCursorProvider
(
	private val contentResolver: ContentResolver,
	private val storedFileProvider: AccessStoredFiles,
	private val cachedFilePropertiesProvider: ProvideLibraryFileProperties
) : ProvideMediaQueryCursor, PromisedResponse<Map<String, String>, Cursor?> {

	override fun getMediaQueryCursor(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Cursor?> =
		storedFileProvider
			.getStoredFile(libraryId, serviceFile)
			.eventually { storedFile ->
				storedFile
					?.uri
					?.let { uri ->
						QueuedPromise(
							MessageWriter {
								contentResolver.query(
									uri.toUri(),
									mediaQueryProjection,
									null,
									null,
									null
								)
							}, ThreadPools.io)
					}
					?: cachedFilePropertiesProvider
					.promiseFileProperties(libraryId, serviceFile)
					.eventually(this)
			}

	override fun promiseResponse(fileProperties: Map<String, String>): Promise<Cursor?> {
		val originalFilename = fileProperties[KnownFileProperties.Filename]
			?: return Promise(IOException("The filename property was not retrieved. A connection needs to be re-established."))

		val filename = FilenameUtils.getBaseName(originalFilename)

		return QueuedPromise(
			MessageWriter {
				contentResolver.query(
					MediaCollections.ExternalAudio,
					mediaQueryProjection,
					mediaDisplayNameQuery,
					arrayOf(filename),
					null
				)
			}, ThreadPools.io)
	}

	companion object {
		private const val mediaIdQuery = MediaStore.Audio.Media._ID + " = ?"
		private const val mediaDisplayNameQuery = MediaStore.Audio.Media.DISPLAY_NAME + " LIKE '%' || ? || '%' "
		private val mediaQueryProjection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
	}
}
