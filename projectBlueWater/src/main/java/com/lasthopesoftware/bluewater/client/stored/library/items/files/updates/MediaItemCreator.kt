package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.albumArtistOrArtist
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.baseFileNameAsMp3
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

private const val audioIdKey = MediaStore.Audio.Media._ID
private val logger by lazyLogger<MediaItemCreator>()
private const val mediaIdQuery = MediaStore.Audio.Media._ID + " = ?"

class MediaItemCreator(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val contentResolver: ContentResolver,
) : CreateExternalMediaItem {
	override fun promiseCreatedItem(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Int?> = CancellableProxyPromise { cp ->
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.also(cp::doCancel)
			.eventually { fileProperties ->
				QueuedPromise(MessageWriter {
					contentResolver
						.takeUnless { cp.isCancelled }
						?.insert(
							MediaCollections.ExternalAudio,
							ContentValues().apply {
								put(MediaStore.Audio.Media.DISPLAY_NAME, fileProperties.baseFileNameAsMp3)
								put(MediaStore.Audio.Media.ARTIST, fileProperties.albumArtistOrArtist)
								put(MediaStore.Audio.Media.ALBUM, fileProperties[KnownFileProperties.Album])
								put(MediaStore.Audio.Media.IS_PENDING, 1)
							}
						)
						?.takeUnless { cp.isCancelled }
						?.let { uri ->
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								contentResolver.refresh(MediaCollections.ExternalAudio, null, null)
							}

							contentResolver.notifyChange(MediaCollections.ExternalAudio, null)

							val maybeCursor = contentResolver
								.query(
									MediaCollections.ExternalAudio,
									null,
									null,
									null,
									null
								)

							maybeCursor
								?.use { cursor ->
									try {
										cursor
											.takeIf { !cp.isCancelled && it.moveToFirst() }
											?.getInt(cursor.getColumnIndexOrThrow(audioIdKey))
									} catch (ie: IllegalArgumentException) {
										logger.info("Illegal column name.", ie)
										null
									}
								}
						}
				}, ThreadPools.io)
			}
	}
}
