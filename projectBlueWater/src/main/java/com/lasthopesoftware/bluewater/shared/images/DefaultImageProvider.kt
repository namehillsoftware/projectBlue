package com.lasthopesoftware.bluewater.shared.images

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.io.ByteArrayOutputStream
import kotlin.math.max

class DefaultImageProvider(private val context: Context) : ProvideDefaultImage {

	override fun promiseImageBytes(): Promise<ByteArray> = promiseFillerBytes(context)

	companion object {
		private lateinit var promisedBytes: Promise<ByteArray>

		@Synchronized
		private fun promiseFillerBytes(context: Context) =
			if (::promisedBytes.isInitialized) promisedBytes
			else QueuedPromise({
				val dm = context.resources.displayMetrics
				val maxSize = max(dm.heightPixels, dm.widthPixels)
				val scaledBitmap = AppCompatResources.getDrawable(context, R.drawable.wave_background)
					?.toBitmap(maxSize, maxSize)
					?: Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ARGB_8888)

				val bitmap = scaledBitmap.config?.let { config -> scaledBitmap.copy(config, false) } ?: scaledBitmap

				val byteArraySize = bitmap.rowBytes * bitmap.height
				ByteArrayOutputStream(byteArraySize).use {
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
					it.toByteArray()
				}
			}, ThreadPools.compute).also { promisedBytes = it }
	}
}
