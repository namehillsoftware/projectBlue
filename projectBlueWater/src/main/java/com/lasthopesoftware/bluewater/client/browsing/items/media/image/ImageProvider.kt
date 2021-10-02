package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import kotlin.coroutines.cancellation.CancellationException

class ImageProvider(private val selectedLibraryId: ProvideSelectedLibraryId, private val rawImages: GetRawImages) : ProvideImages {
	override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
		CancellableProxyPromise { cp ->
			selectedLibraryId.selectedLibraryId
				.eventually { libraryId ->
					libraryId
						?.let { l -> rawImages.promiseImageBytes(l, serviceFile).also(cp::doCancel) }
						?.eventually { bytes -> QueuedPromise(BitmapWriter(bytes), ThreadPools.compute) }
						.keepPromise()
				}
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
