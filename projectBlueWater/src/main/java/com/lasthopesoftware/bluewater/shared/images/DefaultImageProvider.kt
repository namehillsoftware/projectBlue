package com.lasthopesoftware.bluewater.shared.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import kotlin.math.max

class DefaultImageProvider(private val context: Context) : ProvideDefaultImage {
	override fun promiseFileBitmap(): Promise<Bitmap> = promiseFillerBitmap(context)

	companion object {
		private lateinit var promisedBitmap: Promise<Bitmap>

		@Synchronized
		private fun promiseFillerBitmap(context: Context) =
			if (::promisedBitmap.isInitialized) promisedBitmap
			else QueuedPromise(MessageWriter {
				val decodedBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.wave_background)
				val dm = context.resources.displayMetrics
				val maxSize = max(dm.heightPixels, dm.widthPixels)
				val scaledBitmap = Bitmap.createScaledBitmap(decodedBitmap, maxSize, maxSize, false)
				val immutableBitmap = scaledBitmap.copy(scaledBitmap.config, false)
				immutableBitmap
			}, ThreadPools.compute).also { promisedBitmap = it }
	}
}
