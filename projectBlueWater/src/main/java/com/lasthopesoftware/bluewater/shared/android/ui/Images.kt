package com.lasthopesoftware.bluewater.shared.android.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

fun ByteArray.toImageBitmap() = BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
