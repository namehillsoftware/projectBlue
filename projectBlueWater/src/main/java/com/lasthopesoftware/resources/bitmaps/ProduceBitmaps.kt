package com.lasthopesoftware.resources.bitmaps

import android.graphics.Bitmap
import com.namehillsoftware.handoff.promises.Promise

interface ProduceBitmaps {
	fun promiseBitmap(byteArray: ByteArray): Promise<Bitmap?>
}
