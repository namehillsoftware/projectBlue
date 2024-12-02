package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.android.ui.ProvideScreenDimensions
import com.lasthopesoftware.bluewater.shared.images.bytes.GetRawImages
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScaledImageProvider(
	private val innerRawImages: GetRawImages,
	private val screenDimensions: ProvideScreenDimensions
) : GetRawImages, PromisedResponse<ByteArray, ByteArray> {

	companion object {
		private fun Int.scaleInteger(scaleRatio: Double): Int = (this.toDouble() * scaleRatio).roundToInt()

		private fun cancellationException() = CancellationException("Cancelled while scaling bitmap")
	}

	private val maximumScreenDimension by lazy {
		val dm = screenDimensions
		max(dm.heightPixels, dm.widthPixels)
	}

	override fun promiseImageBytes(libraryId: LibraryId, serviceFile: ServiceFile): Promise<ByteArray> =
		Promise.Proxy { cp ->
			innerRawImages.promiseImageBytes(libraryId, serviceFile)
				.also(cp::doCancel)
				.eventually(this)
		}

	override fun promiseImageBytes(libraryId: LibraryId, itemId: ItemId): Promise<ByteArray> =
		Promise.Proxy { cp ->
			innerRawImages.promiseImageBytes(libraryId, itemId)
				.also(cp::doCancel)
				.eventually(this)
		}

	override fun promiseResponse(bytes: ByteArray): Promise<ByteArray> =
		bytes
			.takeIf { it.isNotEmpty() }
			?.let { b -> QueuedPromise(ScaledBitmapMessageWriter(b), ThreadPools.compute) }
			.keepPromise(bytes)

	private inner class ScaledBitmapMessageWriter(private val inputBytes: ByteArray): CancellableMessageWriter<ByteArray> {
		override fun prepareMessage(signal: CancellationSignal): ByteArray {
			if (signal.isCancelled) throw cancellationException()

			return BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)
				?.takeIf { b -> b.width > maximumScreenDimension && b.height > maximumScreenDimension }
				?.let { image ->
					val minimumImageDimension = min(image.width, image.height).toDouble()
					val minimumShrink = maximumScreenDimension / minimumImageDimension

					if (signal.isCancelled) throw cancellationException()

					val scaledBitmap = Bitmap.createScaledBitmap(
						image,
						image.width.scaleInteger(minimumShrink),
						image.height.scaleInteger(minimumShrink),
						true
					)

					if (signal.isCancelled) throw cancellationException()

					val byteArraySize = scaledBitmap.rowBytes * scaledBitmap.height
					val byteBuffer = ByteBuffer.allocate(byteArraySize)
					scaledBitmap.copyPixelsToBuffer(byteBuffer)
					byteBuffer.rewind()
					byteBuffer.array()
				}
				?: inputBytes
		}
	}
}
