package com.lasthopesoftware.resources.bitmaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

object QueuedBitmapProducer : ProduceBitmaps {
	override fun promiseBitmap(byteArray: ByteArray): Promise<Bitmap?> =
		byteArray
			.takeIf { it.isNotEmpty() }
			?.let {
				QueuedPromise(
					{ cs ->
						if (!cs.isCancelled) BitmapFactory.decodeByteArray(it, 0, it.size)
						else null
					},
					ThreadPools.compute
				)
			}
			.keepPromise()
}
