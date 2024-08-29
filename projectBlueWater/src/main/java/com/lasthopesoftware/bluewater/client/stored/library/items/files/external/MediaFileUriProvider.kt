package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.fileNameParts
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.localExternalRelativeFileDirectory
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.MediaCollections
import com.lasthopesoftware.resources.uri.resourceExists
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.apache.commons.io.FilenameUtils
import java.io.IOException

private const val audioIdKey = MediaStore.Audio.Media._ID

private const val DISPLAY_NAME_MEDIA_COLLECTION_FILTER = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"

private const val MEDIA_FILE_FOUND_LOG_MESSAGE = "Returning media file URI {} from local disk."

private val mediaQueryProjection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)

private fun Cursor.takeFirstAudioFile() = use {
	it.takeIf { it.moveToFirst() }
		?.run { getLongOrNull(getColumnIndexOrThrow(audioIdKey)) }
		// The file object will produce a properly escaped file URI, as opposed to what is stored in the DB
		?.let { fileId -> ContentUris.withAppendedId(MediaCollections.ExternalAudio, fileId) }
}

interface MediaFileUriProvider : ProvideFileUrisForLibrary

open class DataFileUriProvider(
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	private val contentResolver: ContentResolver,
) : MediaFileUriProvider, PromisedResponse<Map<String, String>, Uri?> {

    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		if (externalStorageReadPermissionsArbitrator.run { !isReadPermissionGranted && !isReadMediaAudioPermissionGranted }) Promise.empty()
		else getMediaQueryUri(libraryId, serviceFile)

	override fun promiseResponse(fileProperties: Map<String, String>): Promise<Uri?> {
		val (_, baseFileName, _, postExtension) = fileProperties.fileNameParts
			?: throw IOException("The filename property was not retrieved. A connection needs to be re-established.")

		var fileNamePattern = FilenameUtils.concat("%${fileProperties.localExternalRelativeFileDirectory}", "%$baseFileName%.")
		if (postExtension.isNotEmpty()) fileNamePattern += postExtension

		return QueuedPromise(
			MessageWriter {
				val maybeCursor = contentResolver.query(
					MediaCollections.ExternalAudio,
					mediaQueryProjection,
					DISPLAY_NAME_MEDIA_COLLECTION_FILTER,
					arrayOf(fileNamePattern),
					null
				)

				maybeCursor
					?.takeFirstAudioFile()
					?.takeIf(contentResolver::resourceExists)?.also {
						logger.info(MEDIA_FILE_FOUND_LOG_MESSAGE, it)
					}
			}, ThreadPools.io
		)
	}

	private fun getMediaQueryUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		filePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.eventually(this)

	companion object {
        private val logger by lazyLogger<MediaFileUriProvider>()
    }
}

class MetadataMediaFileUriProvider(
	filePropertiesProvider: ProvideLibraryFileProperties,
	externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	private val contentResolver: ContentResolver,
) : DataFileUriProvider(filePropertiesProvider, externalStorageReadPermissionsArbitrator, contentResolver), PromisedResponse<Map<String, String>, Uri?> {
	override fun promiseResponse(fileProperties: Map<String, String>): Promise<Uri?> {
		return QueuedPromise(
			MessageWriter {
				val selectionArgs = arrayOf(
					fileProperties[KnownFileProperties.Artist] ?: "",
					fileProperties[KnownFileProperties.AlbumArtist] ?: "",
					fileProperties[KnownFileProperties.Name] ?: "",
					fileProperties[KnownFileProperties.Album] ?: ""
				)

				val maybeCursor = contentResolver.query(
					MediaCollections.ExternalAudio,
					mediaQueryProjection,
					META_DATA_FILTER,
					selectionArgs,
					null
				)

				maybeCursor
					?.takeFirstAudioFile()
					?.takeIf(contentResolver::resourceExists)?.also {
						logger.info(MEDIA_FILE_FOUND_LOG_MESSAGE, it)
					}
			}, ThreadPools.io
		).eventually { uri ->
			uri?.toPromise() ?: super.promiseResponse(fileProperties)
		}
	}

	companion object {
		private val logger by lazyLogger<MetadataMediaFileUriProvider>()

		private const val META_DATA_FILTER = """${MediaStore.Audio.Media.IS_PENDING} = 0
				AND COALESCE(${MediaStore.Audio.Media.ARTIST}, "") = ?
				AND COALESCE(${MediaStore.Audio.Media.ALBUM_ARTIST}, "") = ?
				AND COALESCE(${MediaStore.Audio.Media.TITLE}, "") = ?
				AND COALESCE(${MediaStore.Audio.AlbumColumns.ALBUM}, "") = ?"""
	}
}

class CompatibleMediaFileUriProvider(
	cachedFilePropertiesProvider: ProvideLibraryFileProperties,
	externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	contentResolver: ContentResolver,
) : MediaFileUriProvider {
	private val inner =
		if (Build.VERSION.SDK_INT >= 29) MetadataMediaFileUriProvider(cachedFilePropertiesProvider, externalStorageReadPermissionsArbitrator, contentResolver)
		else DataFileUriProvider(cachedFilePropertiesProvider, externalStorageReadPermissionsArbitrator, contentResolver)

	override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		inner.promiseUri(libraryId, serviceFile)
}
