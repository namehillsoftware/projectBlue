package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.toURI
import com.lasthopesoftware.resources.uri.toUri
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.io.File
import java.net.URI

class ExternalContentRepository(
	private val contentResolver: ContentResolver,
	private val context: Context,
) : HaveExternalContent {

	override fun promiseNewContentUri(externalContent: ExternalContent): Promise<URI?> =
		QueuedPromise(CancellableMessageWriter { ct ->
			externalContent
				.takeUnless { ct.isCancelled }
				?.toContentValues()
				?.takeUnless { ct.isCancelled }
				?.let { newContent ->
					if (Build.VERSION.SDK_INT < 29) {
						val root = context.getExternalFilesDir(externalContent.type)?.path

						val relativePath = newContent.getAsString(MediaStore.Audio.Media.RELATIVE_PATH)
						val albumDirectory = relativePath?.let { File(root, it).path } ?: root
						val songFile = File(albumDirectory, newContent.getAsString(MediaStore.Audio.Media.DISPLAY_NAME))
						newContent.put(MediaStore.Audio.Media.DATA, songFile.path)

						newContent.remove(MediaStore.Audio.Media.RELATIVE_PATH)
						newContent.remove(MediaStore.Audio.Media.IS_PENDING)

						contentResolver.insert(externalContent.collection, newContent)
						songFile.toURI()
					} else {
						contentResolver.insert(externalContent.collection, newContent)?.toURI()
					}
				}
		}, ThreadPools.io)

	override fun markContentAsNotPending(uri: URI): Promise<Unit> = QueuedPromise(MessageWriter{
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

	override fun removeContent(uri: URI): Promise<Boolean> = QueuedPromise(MessageWriter{
		val deletedRecords = contentResolver.delete(uri.toUri(), null, null)
		deletedRecords > 0
	}, ThreadPools.io)
}
