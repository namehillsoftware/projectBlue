/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.lasthopesoftware.bluewater.shared.android.colors

import android.graphics.*
import kotlin.math.abs

/**
 * Utility class for image analysis and processing.
 *
 * @hide
 */
class ImageUtils {
	private var mTempBuffer: IntArray = IntArray(0)
	private val mTempCompactBitmap by lazy { Bitmap.createBitmap(COMPACT_BITMAP_SIZE, COMPACT_BITMAP_SIZE, Bitmap.Config.ARGB_8888) }
	private val mTempCompactBitmapCanvas by lazy { Canvas(mTempCompactBitmap) }
	private val mTempCompactBitmapPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true } }
	private val mTempMatrix = Matrix()

	/**
	 * Checks whether a bitmap is grayscale. Grayscale here means "very close to a perfect
	 * gray".
	 *
	 *
	 * Instead of scanning every pixel in the bitmap, we first resize the bitmap to no more than
	 * COMPACT_BITMAP_SIZE^2 pixels using filtering. The hope is that any non-gray color elements
	 * will survive the squeezing process, contaminating the result with color.
	 */
	fun isGrayscale(bitmap: Bitmap): Boolean {
		fun ensureBufferSize(size: Int) {
			if (mTempBuffer.size < size) {
				mTempBuffer = IntArray(size)
			}
		}

		var bitmap = bitmap

		var height = bitmap.height
		var width = bitmap.width

		// shrink to a more manageable (yet hopefully no more or less colorful) size
		if (height > COMPACT_BITMAP_SIZE || width > COMPACT_BITMAP_SIZE) {
			mTempMatrix.reset()
			mTempMatrix.setScale(
				COMPACT_BITMAP_SIZE.toFloat() / width,
				COMPACT_BITMAP_SIZE.toFloat() / height, 0f, 0f
			)
			mTempCompactBitmapCanvas.drawColor(0, PorterDuff.Mode.SRC) // select all, erase
			mTempCompactBitmapCanvas.drawBitmap(bitmap, mTempMatrix, mTempCompactBitmapPaint)
			bitmap = mTempCompactBitmap
			height = COMPACT_BITMAP_SIZE
			width = height
		}
		val size = height * width
		ensureBufferSize(size)
		bitmap.getPixels(mTempBuffer, 0, width, 0, 0, width, height)
		for (i in 0 until size) {
			if (!isGrayscale(mTempBuffer[i])) {
				return false
			}
		}
		return true
	}

	companion object {
		// Amount (max is 255) that two channels can differ before the color is no longer "gray".
		private const val TOLERANCE = 20

		// Alpha amount for which values below are considered transparent.
		private const val ALPHA_TOLERANCE = 50

		// Size of the smaller bitmap we're actually going to scan.
		private const val COMPACT_BITMAP_SIZE = 64 // pixels

		/**
		 * Classifies a color as grayscale or not. Grayscale here means "very close to a perfect
		 * gray"; if all three channels are approximately equal, this will return true.
		 *
		 *
		 * Note that really transparent colors are always grayscale.
		 */
		fun isGrayscale(color: Int): Boolean {
			val alpha = 0xFF and (color shr 24)
			if (alpha < ALPHA_TOLERANCE) {
				return true
			}
			val r = 0xFF and (color shr 16)
			val g = 0xFF and (color shr 8)
			val b = 0xFF and color
			return abs(r - g) < TOLERANCE && abs(r - b) < TOLERANCE && abs(g - b) < TOLERANCE
		}
	}
}
