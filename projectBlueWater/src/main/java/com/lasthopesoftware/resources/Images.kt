package com.lasthopesoftware.resources

import android.graphics.Bitmap

inline fun <T> Bitmap.use(usage: (Bitmap) -> T) = try {
	usage(this)
} finally {
    recycle()
}
