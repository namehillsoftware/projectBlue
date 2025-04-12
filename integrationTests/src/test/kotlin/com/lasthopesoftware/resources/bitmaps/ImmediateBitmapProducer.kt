package com.lasthopesoftware.resources.bitmaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

object ImmediateBitmapProducer : ProduceBitmaps {
	override fun promiseBitmap(byteArray: ByteArray): Promise<Bitmap?> =
		byteArray
			.takeIf { it.isNotEmpty() }
			?.let {
				BitmapFactory.decodeByteArray(it, 0, it.size).toPromise()
			}
			.keepPromise()
}
