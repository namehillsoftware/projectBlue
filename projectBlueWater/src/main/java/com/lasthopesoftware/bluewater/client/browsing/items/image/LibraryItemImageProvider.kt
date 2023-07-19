package com.lasthopesoftware.bluewater.client.browsing.items.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.images.bytes.GetRawImages
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import java.util.concurrent.CancellationException

class LibraryItemImageProvider(private val rawImages: GetRawImages) : ProvideLibraryItemImages {
	override fun promiseImage(libraryId: LibraryId, itemId: ItemId): Promise<Bitmap?> = CancellableProxyPromise { cp ->
		rawImages
			.promiseImageBytes(libraryId, itemId)
			.also(cp::doCancel)
			.eventually { bytes -> QueuedPromise(BitmapWriter(bytes), ThreadPools.compute) }
	}

	private class BitmapWriter(private val imageBytes: ByteArray) : CancellableMessageWriter<Bitmap?> {
		override fun prepareMessage(cancellationToken: CancellationToken): Bitmap? =
			when {
				cancellationToken.isCancelled -> throw CancellationException("Cancelled while decoding image")
				imageBytes.isNotEmpty() -> BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
				else -> null
			}
	}
}
