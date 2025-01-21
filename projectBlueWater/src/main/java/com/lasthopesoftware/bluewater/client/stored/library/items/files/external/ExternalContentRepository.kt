package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.toURI
import com.lasthopesoftware.resources.uri.toUri
import com.lasthopesoftware.storage.directories.GetPublicDirectories
import com.namehillsoftware.handoff.promises.Promise
import java.io.File
import java.net.URI

class ExternalContentRepository(
	private val contentResolver: ContentResolver,
	private val publicDirectoryLookup: GetPublicDirectories,
) : HaveExternalContent {

	override fun promiseNewContentUri(externalContent: ExternalContent): Promise<URI?> =
		externalContent
			.toContentValues()
			.let { newContent ->
				if (Build.VERSION.SDK_INT >= 29) ThreadPools.io.preparePromise { ct ->
					if (!ct.isCancelled)
						contentResolver.insert(externalContent.collection, newContent)?.toURI()
					else
						null
				} else publicDirectoryLookup.promisePublicDrives().eventually { drives ->
					ThreadPools.io.preparePromise { ct ->
						drives
							.firstOrNull { d -> d.exists() }
							?.path
							?.takeUnless { ct.isCancelled }
							?.let { root ->
								val relativePath = newContent.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)
								val albumDirectory = relativePath?.let { File(root, it).path } ?: root
								val file = File(albumDirectory, newContent.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
								newContent.put(MediaStore.MediaColumns.DATA, file.path)

								newContent.remove(MediaStore.MediaColumns.RELATIVE_PATH)
								newContent.remove(MediaStore.MediaColumns.IS_PENDING)

								if (ct.isCancelled) null
								else {
									contentResolver.insert(externalContent.collection, newContent)
									file.toURI()
								}
							}
					}
				}
			}

	override fun markContentAsNotPending(uri: URI): Promise<Unit> = ThreadPools.io.preparePromise {
		contentResolver.update(
			uri.toUri(),
			ContentValues().apply {
				put(MediaStore.Audio.Media.IS_PENDING, 0)
			},
			null,
			null
		)

		Unit
	}

	override fun removeContent(uri: URI): Promise<Boolean> = ThreadPools.io.preparePromise {
		val deletedRecords = contentResolver.delete(uri.toUri(), null, null)
		deletedRecords > 0
	}
}
