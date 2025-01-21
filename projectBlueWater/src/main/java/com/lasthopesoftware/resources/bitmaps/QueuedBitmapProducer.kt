package com.lasthopesoftware.resources.bitmaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise

object QueuedBitmapProducer : ProduceBitmaps {
	override fun promiseBitmap(byteArray: ByteArray): Promise<Bitmap?> =
		byteArray
			.takeIf { it.isNotEmpty() }
			?.let {
				ThreadPools.compute.preparePromise { cs ->
					if (!cs.isCancelled) BitmapFactory.decodeByteArray(it, 0, it.size)
					else null
				}
			}
			.keepPromise()
}
