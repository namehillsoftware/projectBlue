package com.lasthopesoftware.bluewater.shared.images

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.nio.ByteBuffer
import kotlin.math.max

class DefaultImageProvider(private val context: Context) : ProvideDefaultImage {

	override fun promiseImageBytes(): Promise<ByteArray> = promiseFillerBytes(context)

	companion object {
		private lateinit var promisedBitmap: Promise<Bitmap>
		private lateinit var promisedBytes: Promise<ByteArray>

		@Synchronized
		private fun promiseFillerBitmap(context: Context) =
			if (::promisedBitmap.isInitialized) promisedBitmap
			else QueuedPromise(MessageWriter {
				val dm = context.resources.displayMetrics
				val maxSize = max(dm.heightPixels, dm.widthPixels)
				val scaledBitmap = AppCompatResources.getDrawable(context, R.drawable.wave_background)
					?.toBitmap(maxSize, maxSize)
					?: Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ARGB_8888)

				scaledBitmap.config?.let { config -> scaledBitmap.copy(config, false) } ?: scaledBitmap
			}, ThreadPools.compute).also { promisedBitmap = it }

		@Synchronized
		private fun promiseFillerBytes(context: Context) =
			if (::promisedBytes.isInitialized) promisedBytes
			else promiseFillerBitmap(context)
				.eventually { bitmap ->
					QueuedPromise(MessageWriter {
						val byteArraySize = bitmap.rowBytes * bitmap.height
						val byteBuffer = ByteBuffer.allocate(byteArraySize)
						bitmap.copyPixelsToBuffer(byteBuffer)
						byteBuffer.rewind()
						byteBuffer.array()
					}, ThreadPools.compute).also { promisedBytes = it }
				}
	}
}
