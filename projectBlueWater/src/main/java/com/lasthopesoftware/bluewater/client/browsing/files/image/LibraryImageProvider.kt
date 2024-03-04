package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.bytes.GetRawImages
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import java.util.concurrent.CancellationException

class LibraryImageProvider(private val rawImages: GetRawImages) : ProvideLibraryImages {
	override fun promiseFileBitmap(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Bitmap?> = CancellableProxyPromise { cp ->
		rawImages
			.promiseImageBytes(libraryId, serviceFile)
			.also(cp::doCancel)
			.eventually { bytes -> QueuedPromise(BitmapWriter(bytes), ThreadPools.compute) }
	}

	private class BitmapWriter(private val imageBytes: ByteArray) : CancellableMessageWriter<Bitmap?> {
		override fun prepareMessage(cancellationToken: CancellationSignal): Bitmap? =
			when {
				cancellationToken.isCancelled -> throw CancellationException("Cancelled while decoding image")
				imageBytes.isNotEmpty() -> BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
				else -> null
			}
	}
}
