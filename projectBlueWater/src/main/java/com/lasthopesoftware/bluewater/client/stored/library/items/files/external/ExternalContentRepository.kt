package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.toURI
import com.lasthopesoftware.resources.uri.toUri
import com.lasthopesoftware.storage.directories.GetPublicDirectories
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
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
				if (Build.VERSION.SDK_INT >= 29) QueuedPromise({ ct ->
					if (!ct.isCancelled)
						contentResolver.insert(externalContent.collection, newContent)?.toURI()
					else
						null
				}, ThreadPools.io) else publicDirectoryLookup.promisePublicDrives().eventually { drives ->
					QueuedPromise({ ct ->
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
					}, ThreadPools.io)
				}
			}

	override fun markContentAsNotPending(uri: URI): Promise<Unit> = QueuedPromise({
		contentResolver.update(
			uri.toUri(),
			ContentValues().apply {
				put(MediaStore.Audio.Media.IS_PENDING, 0)
			},
			null,
			null
		)

		Unit
	}, ThreadPools.io)

	override fun removeContent(uri: URI): Promise<Boolean> = QueuedPromise({
		val deletedRecords = contentResolver.delete(uri.toUri(), null, null)
		deletedRecords > 0
	}, ThreadPools.io)
}
