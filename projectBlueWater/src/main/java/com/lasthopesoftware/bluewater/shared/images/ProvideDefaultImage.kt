package com.lasthopesoftware.bluewater.shared.images

import android.graphics.Bitmap
import com.namehillsoftware.handoff.promises.Promise

interface ProvideDefaultImage {
	fun promiseFileBitmap(): Promise<Bitmap>
}
