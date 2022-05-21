package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.content.Context
import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScaledImageProvider(private val inner: ProvideImages, private val context: Context) : ProvideImages {
	private val maximumScreenDimension by lazy {
		val dm = context.resources.displayMetrics
		max(dm.heightPixels, dm.widthPixels)
	}

	override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
		CancellableProxyPromise { cp ->
			inner.promiseFileBitmap(serviceFile)
				.also(cp::doCancel)
				.eventually { image ->
					if (cp.isCancelled) Promise(cancellationException())
					else image
						?.takeIf { b -> b.width > maximumScreenDimension && b.height > maximumScreenDimension }
						?.let { b ->
							QueuedPromise(MessageWriter {
								if (cp.isCancelled) throw cancellationException()

								val minimumImageDimension = min(b.width, b.height).toDouble()
								val minimumShrink = maximumScreenDimension / minimumImageDimension

								if (cp.isCancelled) throw cancellationException()

								Bitmap.createScaledBitmap(
									b,
									b.width.scaleInteger(minimumShrink),
									b.height.scaleInteger(minimumShrink),
									true
								)
							}, ThreadPools.compute)
						}
						.keepPromise(image)
				}
		}

	companion object {
		private fun Int.scaleInteger(scaleRatio: Double): Int = (this.toDouble() * scaleRatio).roundToInt()

		private fun cancellationException() = CancellationException(("Cancelled while scaling bitmap"))
	}
}
