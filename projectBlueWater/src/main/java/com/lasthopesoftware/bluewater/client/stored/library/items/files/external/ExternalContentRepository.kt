package com.lasthopesoftware.bluewater.client.stored.library.items.files.external

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.uri.toURI
import com.lasthopesoftware.resources.uri.toUri
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.net.URI

class ExternalContentRepository(
	private val contentResolver: ContentResolver,
) : HaveExternalContent {

	override fun promiseNewContentUri(externalContent: ExternalContent): Promise<URI?> =
		QueuedPromise(CancellableMessageWriter { ct ->
			externalContent
				.takeUnless { ct.isCancelled }
				?.toContentValues()
				?.takeUnless { ct.isCancelled }
				?.let { newContent ->
					contentResolver.insert(externalContent.collection, newContent)
				}
				?.toURI()
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
