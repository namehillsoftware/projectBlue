package com.lasthopesoftware.bluewater.client.browsing.items.media.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.resources.scheduling.ParsingScheduler
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

open class ImageProvider(private val rawImages: GetRawImages) {
	open fun promiseFileBitmap(serviceFile: ServiceFile?): Promise<Bitmap?>? {
		return rawImages.promiseImageBytes(serviceFile!!)
			.eventually { bytes: ByteArray -> QueuedPromise(BitmapWriter(bytes), ParsingScheduler.instance().scheduler) }
	}

	private class BitmapWriter internal constructor(private val imageBytes: ByteArray) : MessageWriter<Bitmap?> {
		override fun prepareMessage(): Bitmap? {
			return if (imageBytes.isNotEmpty()) getBitmapFromBytes(imageBytes) else null
		}
	}

	companion object {
		private fun getBitmapFromBytes(imageBytes: ByteArray): Bitmap {
			return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
		}
	}
}
