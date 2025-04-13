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
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.MediaCollections
import com.lasthopesoftware.resources.uri.resourceExists
import com.namehillsoftware.handoff.promises.Promise
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

abstract class PermissionsCheckingMediaFileUriProvider(
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val externalStorageReadPermissionsArbitrator: CheckOsPermissions,
): MediaFileUriProvider, PromisedResponse<Map<String, String>, Uri?> {
	override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		if (externalStorageReadPermissionsArbitrator.run { !isReadPermissionGranted && !isReadMediaAudioPermissionGranted }) Promise.empty()
		else getMediaQueryUri(libraryId, serviceFile)

	private fun getMediaQueryUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		filePropertiesProvider
			.promiseFileProperties(libraryId, serviceFile)
			.eventually(this)
}

class DataFileUriProvider(
	filePropertiesProvider: ProvideLibraryFileProperties,
	externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	private val contentResolver: ContentResolver,
) : PermissionsCheckingMediaFileUriProvider(filePropertiesProvider, externalStorageReadPermissionsArbitrator), PromisedResponse<Map<String, String>, Uri?> {

	override fun promiseResponse(fileProperties: Map<String, String>): Promise<Uri?> {
		val (_, baseFileName, _, postExtension) = fileProperties.fileNameParts
			?: throw IOException("The filename property was not retrieved. A connection needs to be re-established.")

		val fileNamePattern = FilenameUtils.concat(
				"%${fileProperties.localExternalRelativeFileDirectory}",
				if (postExtension.isEmpty()) "%$baseFileName%." else "%$postExtension%.")

		return ThreadPools.io.preparePromise {
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
		}
	}

	companion object {
        private val logger by lazyLogger<DataFileUriProvider>()
    }
}

class MetadataMediaFileUriProvider(
	filePropertiesProvider: ProvideLibraryFileProperties,
	externalStorageReadPermissionsArbitrator: CheckOsPermissions,
	private val contentResolver: ContentResolver,
) : PermissionsCheckingMediaFileUriProvider(filePropertiesProvider, externalStorageReadPermissionsArbitrator), PromisedResponse<Map<String, String>, Uri?> {

	override fun promiseResponse(fileProperties: Map<String, String>): Promise<Uri?> = ThreadPools.io.preparePromise {
		val selectionArgs = arrayOf(
			fileProperties[NormalizedFileProperties.Artist] ?: "",
			fileProperties[NormalizedFileProperties.AlbumArtist] ?: "",
			fileProperties[NormalizedFileProperties.Name] ?: "",
			fileProperties[NormalizedFileProperties.Album] ?: ""
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
	private val inner: MediaFileUriProvider =
		if (Build.VERSION.SDK_INT >= 29) MetadataMediaFileUriProvider(cachedFilePropertiesProvider, externalStorageReadPermissionsArbitrator, contentResolver)
		else DataFileUriProvider(cachedFilePropertiesProvider, externalStorageReadPermissionsArbitrator, contentResolver)

	override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		inner.promiseUri(libraryId, serviceFile)
}
