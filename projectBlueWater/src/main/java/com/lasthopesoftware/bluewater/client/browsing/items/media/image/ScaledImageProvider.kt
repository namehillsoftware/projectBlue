package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.util.DisplayMetrics
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise
import kotlin.math.roundToInt

class ScaledImageProvider(private val inner: ProvideImages, private val activity: Activity) : ProvideImages {
	private val displayMetrics by lazy {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			val metrics = activity.windowManager.maximumWindowMetrics
			Pair(metrics.bounds.width(), metrics.bounds.height())
		} else {
			val displayMetrics = DisplayMetrics()
			activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
			Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
		}
	}

	override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
		inner.promiseFileBitmap(serviceFile)
			.then { image ->
				image
					?.takeUnless { b -> b.width < displayMetrics.first && b.height < displayMetrics.second }
					?.let { b ->
						val (width, height) = displayMetrics
						val minShrink = minOf(b.width.toDouble() / width, b.height.toDouble() / height)
						Bitmap.createScaledBitmap(
							b,
							width.scaleInteger(minShrink),
							height.scaleInteger(minShrink),
							true)
					}
			}

	companion object {
		private fun Int.scaleInteger(scaleRatio: Double): Int =
			(this.toDouble() * scaleRatio).roundToInt()
	}
}
