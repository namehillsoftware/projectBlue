package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.util.DisplayMetrics
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class ScaledImageProvider(private val inner: ProvideImages, private val activity: Activity) : ProvideImages {
	private val displayMetrics by lazy {
		val windowManager = activity.windowManager
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			val metrics = windowManager.maximumWindowMetrics
			Pair(metrics.bounds.width(), metrics.bounds.height())
		} else {
			val displayMetrics = DisplayMetrics()
			windowManager.defaultDisplay.getRealMetrics(displayMetrics)
			Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
		}
	}

	override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
		CancellableProxyPromise { cp ->
			inner.promiseFileBitmap(serviceFile)
				.also(cp::doCancel)
				.eventually { image ->
					image
						?.takeUnless { b -> b.width < displayMetrics.first || b.height < displayMetrics.second }
						?.let { b ->
							QueuedPromise(MessageWriter {
								if (cp.isCancelled) throw CancellationException("Cancelled while scaling bitmap")

								val (width, height) = displayMetrics
								val shrink = maxOf(
									b.width.toDouble() / width.toDouble(),
									b.height.toDouble() / height.toDouble()
								)

								if (cp.isCancelled) throw CancellationException("Cancelled while scaling bitmap")

								Bitmap.createScaledBitmap(
									b,
									width.scaleInteger(shrink),
									height.scaleInteger(shrink),
									true
								)
							}, ThreadPools.compute)
						}
						.keepPromise()
				}
		}

	companion object {
		private fun Int.scaleInteger(scaleRatio: Double): Int =
			(this.toDouble() * scaleRatio).roundToInt()
	}
}
