package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class SharedStoredFilePathLookup(
	private val libraryFileProperties: ProvideLibraryFileProperties,
	private val context: Context
) : GetSharedStoredFilePaths {
	override fun promiseSharedStoredFilePath(libraryId: LibraryId, serviceFile: ServiceFile): Promise<String?> =
		libraryFileProperties
			.promiseFileProperties(libraryId, serviceFile)
			.eventually { p ->
				QueuedPromise(MessageWriter {
					val contentValues = ContentValues().apply {
						val fileName = p[KnownFileProperties.FILENAME]?.let { f ->
							var lastPathIndex = f.lastIndexOf('\\')
							if (lastPathIndex < 0) lastPathIndex = f.lastIndexOf('/')
							if (lastPathIndex < 0) f
							else {
								var newFileName = f.substring(lastPathIndex + 1)
								val extensionIndex = newFileName.lastIndexOf('.')
								if (extensionIndex > -1)
									newFileName = newFileName.substring(0, extensionIndex + 1) + "mp3"
								newFileName
							}
						}

						put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
						put(MediaStore.Audio.Media.ALBUM, p[KnownFileProperties.ALBUM])
						put(MediaStore.Audio.Media.ARTIST, p[KnownFileProperties.ARTIST])
						put(MediaStore.Audio.Media.TRACK, p[KnownFileProperties.TRACK])
						put(MediaStore.Audio.Media.DURATION, p[KnownFileProperties.TRACK])

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
							put(MediaStore.Audio.Media.ALBUM_ARTIST, p[KnownFileProperties.ALBUM_ARTIST])
						}
					}

					val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
					} else {
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
					}

					context.contentResolver.insert(contentUri, contentValues)?.path
				}, ThreadPools.io)
			}
}
