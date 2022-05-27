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

import android.app.Notification
import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import kotlin.math.*

/**
 * Helper class to process legacy (Holo) notifications to make them look like material notifications.
 *
 */
object NotificationColorUtil {

	/**
	 * Framework copy of functions needed from android.support.v4.graphics.ColorUtils.
	 */
	private object ColorUtilsFromCompat {
		private const val XYZ_WHITE_REFERENCE_X = 95.047
		private const val XYZ_WHITE_REFERENCE_Y = 100.0
		private const val XYZ_WHITE_REFERENCE_Z = 108.883
		private const val XYZ_EPSILON = 0.008856
		private const val XYZ_KAPPA = 903.3
		private val TEMP_ARRAY = ThreadLocal<DoubleArray>()

		/**
		 * Composite two potentially translucent colors over each other and returns the result.
		 */
		fun compositeColors(@ColorInt foreground: Int, @ColorInt background: Int): Int {
			val bgAlpha = Color.alpha(background)
			val fgAlpha = Color.alpha(foreground)
			val a = compositeAlpha(fgAlpha, bgAlpha)
			val r = compositeComponent(
				Color.red(foreground), fgAlpha,
				Color.red(background), bgAlpha, a
			)
			val g = compositeComponent(
				Color.green(foreground), fgAlpha,
				Color.green(background), bgAlpha, a
			)
			val b = compositeComponent(
				Color.blue(foreground), fgAlpha,
				Color.blue(background), bgAlpha, a
			)
			return Color.argb(a, r, g, b)
		}

		private fun compositeAlpha(foregroundAlpha: Int, backgroundAlpha: Int): Int {
			return 0xFF - (0xFF - backgroundAlpha) * (0xFF - foregroundAlpha) / 0xFF
		}

		private fun compositeComponent(fgC: Int, fgA: Int, bgC: Int, bgA: Int, a: Int): Int {
			return if (a == 0) 0 else (0xFF * fgC * fgA + bgC * bgA * (0xFF - fgA)) / (a * 0xFF)
		}

		/**
		 * Returns the luminance of a color as a float between `0.0` and `1.0`.
		 *
		 * Defined as the Y component in the XYZ representation of `color`.
		 */
		@FloatRange(from = 0.0, to = 1.0)
		fun calculateLuminance(@ColorInt color: Int): Double {
			val result = tempDouble3Array
			colorToXYZ(color, result)
			// Luminance is the Y component
			return result[1] / 100
		}

		/**
		 * Returns the contrast ratio between `foreground` and `background`.
		 * `background` must be opaque.
		 *
		 *
		 * Formula defined
		 * [here](http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef).
		 */
		fun calculateContrast(@ColorInt foreground: Int, @ColorInt background: Int): Double {
			var localForeground = foreground
			if (Color.alpha(background) != 255) {
				Log.wtf(
					TAG,
					"background can not be translucent: #" + Integer.toHexString(background)
				)
			}

			if (Color.alpha(localForeground) < 255) {
				// If the foreground is translucent, composite the foreground over the background
				localForeground = compositeColors(localForeground, background)
			}

			val luminance1 = calculateLuminance(localForeground) + 0.05
			val luminance2 = calculateLuminance(background) + 0.05

			// Now return the lighter luminance divided by the darker luminance
			return max(luminance1, luminance2) / min(luminance1, luminance2)
		}

		/**
		 * Convert the ARGB color to its CIE Lab representative components.
		 *
		 * @param color  the ARGB color to convert. The alpha component is ignored
		 * @param outLab 3-element array which holds the resulting LAB components
		 */
		fun colorToLAB(@ColorInt color: Int, outLab: DoubleArray) {
			RGBToLAB(Color.red(color), Color.green(color), Color.blue(color), outLab)
		}

		/**
		 * Convert RGB components to its CIE Lab representative components.
		 *
		 *
		 *  * outLab[0] is L [0 ...100)
		 *  * outLab[1] is a [-128...127)
		 *  * outLab[2] is b [-128...127)
		 *
		 *
		 * @param r      red component value [0..255]
		 * @param g      green component value [0..255]
		 * @param b      blue component value [0..255]
		 * @param outLab 3-element array which holds the resulting LAB components
		 */
		fun RGBToLAB(
			@IntRange(from = 0x0, to = 0xFF) r: Int,
			@IntRange(from = 0x0, to = 0xFF) g: Int, @IntRange(from = 0x0, to = 0xFF) b: Int,
			outLab: DoubleArray
		) {
			// First we convert RGB to XYZ
			RGBToXYZ(r, g, b, outLab)
			// outLab now contains XYZ
			XYZToLAB(outLab[0], outLab[1], outLab[2], outLab)
			// outLab now contains LAB representation
		}

		/**
		 * Convert the ARGB color to it's CIE XYZ representative components.
		 *
		 *
		 * The resulting XYZ representation will use the D65 illuminant and the CIE
		 * 2° Standard Observer (1931).
		 *
		 *
		 *  * outXyz[0] is X [0 ...95.047)
		 *  * outXyz[1] is Y [0...100)
		 *  * outXyz[2] is Z [0...108.883)
		 *
		 *
		 * @param color  the ARGB color to convert. The alpha component is ignored
		 * @param outXyz 3-element array which holds the resulting LAB components
		 */
		fun colorToXYZ(@ColorInt color: Int, outXyz: DoubleArray) {
			RGBToXYZ(Color.red(color), Color.green(color), Color.blue(color), outXyz)
		}

		/**
		 * Convert RGB components to it's CIE XYZ representative components.
		 *
		 *
		 * The resulting XYZ representation will use the D65 illuminant and the CIE
		 * 2° Standard Observer (1931).
		 *
		 *
		 *  * outXyz[0] is X [0 ...95.047)
		 *  * outXyz[1] is Y [0...100)
		 *  * outXyz[2] is Z [0...108.883)
		 *
		 *
		 * @param r      red component value [0..255]
		 * @param g      green component value [0..255]
		 * @param b      blue component value [0..255]
		 * @param outXyz 3-element array which holds the resulting XYZ components
		 */
		fun RGBToXYZ(
			@IntRange(from = 0x0, to = 0xFF) r: Int,
			@IntRange(from = 0x0, to = 0xFF) g: Int, @IntRange(from = 0x0, to = 0xFF) b: Int,
			outXyz: DoubleArray
		) {
			require(outXyz.size == 3) { "outXyz must have a length of 3." }
			var sr = r / 255.0
			sr = if (sr < 0.04045) sr / 12.92 else ((sr + 0.055) / 1.055).pow(2.4)
			var sg = g / 255.0
			sg = if (sg < 0.04045) sg / 12.92 else ((sg + 0.055) / 1.055).pow(2.4)
			var sb = b / 255.0
			sb = if (sb < 0.04045) sb / 12.92 else ((sb + 0.055) / 1.055).pow(2.4)
			outXyz[0] = 100 * (sr * 0.4124 + sg * 0.3576 + sb * 0.1805)
			outXyz[1] = 100 * (sr * 0.2126 + sg * 0.7152 + sb * 0.0722)
			outXyz[2] = 100 * (sr * 0.0193 + sg * 0.1192 + sb * 0.9505)
		}

		/**
		 * Converts a color from CIE XYZ to CIE Lab representation.
		 *
		 *
		 * This method expects the XYZ representation to use the D65 illuminant and the CIE
		 * 2° Standard Observer (1931).
		 *
		 *
		 *  * outLab[0] is L [0 ...100)
		 *  * outLab[1] is a [-128...127)
		 *  * outLab[2] is b [-128...127)
		 *
		 *
		 * @param x      X component value [0...95.047)
		 * @param y      Y component value [0...100)
		 * @param z      Z component value [0...108.883)
		 * @param outLab 3-element array which holds the resulting Lab components
		 */
		fun XYZToLAB(
			@FloatRange(from = 0.0, to = XYZ_WHITE_REFERENCE_X) x: Double,
			@FloatRange(from = 0.0, to = XYZ_WHITE_REFERENCE_Y) y: Double,
			@FloatRange(from = 0.0, to = XYZ_WHITE_REFERENCE_Z) z: Double,
			outLab: DoubleArray
		) {
			require(outLab.size == 3) { "outLab must have a length of 3." }
			val pivotX = pivotXyzComponent(x / XYZ_WHITE_REFERENCE_X)
			val pivotY = pivotXyzComponent(y / XYZ_WHITE_REFERENCE_Y)
			val pivotZ = pivotXyzComponent(z / XYZ_WHITE_REFERENCE_Z)
			outLab[0] = 0.0.coerceAtLeast(116 * pivotY - 16)
			outLab[1] = 500 * (pivotX - pivotY)
			outLab[2] = 200 * (pivotY - pivotZ)
		}

		/**
		 * Converts a color from CIE Lab to CIE XYZ representation.
		 *
		 *
		 * The resulting XYZ representation will use the D65 illuminant and the CIE
		 * 2° Standard Observer (1931).
		 *
		 *
		 *  * outXyz[0] is X [0 ...95.047)
		 *  * outXyz[1] is Y [0...100)
		 *  * outXyz[2] is Z [0...108.883)
		 *
		 *
		 * @param l      L component value [0...100)
		 * @param a      A component value [-128...127)
		 * @param b      B component value [-128...127)
		 * @param outXyz 3-element array which holds the resulting XYZ components
		 */
		fun LABToXYZ(
			@FloatRange(from = 0.0, to = 100.0) l: Double,
			@FloatRange(from = -128.0, to = 127.0) a: Double,
			@FloatRange(from = -128.0, to = 127.0) b: Double,
			outXyz: DoubleArray
		) {
			val fy = (l + 16) / 116
			val fx = a / 500 + fy
			val fz = fy - b / 200
			var tmp = fx.pow(3.0)
			val xr = if (tmp > XYZ_EPSILON) tmp else (116 * fx - 16) / XYZ_KAPPA
			val yr = if (l > XYZ_KAPPA * XYZ_EPSILON) Math.pow(fy, 3.0) else l / XYZ_KAPPA
			tmp = fz.pow(3.0)
			val zr = if (tmp > XYZ_EPSILON) tmp else (116 * fz - 16) / XYZ_KAPPA
			outXyz[0] = xr * XYZ_WHITE_REFERENCE_X
			outXyz[1] = yr * XYZ_WHITE_REFERENCE_Y
			outXyz[2] = zr * XYZ_WHITE_REFERENCE_Z
		}

		/**
		 * Converts a color from CIE XYZ to its RGB representation.
		 *
		 *
		 * This method expects the XYZ representation to use the D65 illuminant and the CIE
		 * 2° Standard Observer (1931).
		 *
		 * @param x X component value [0...95.047)
		 * @param y Y component value [0...100)
		 * @param z Z component value [0...108.883)
		 * @return int containing the RGB representation
		 */
		@ColorInt
		fun XYZToColor(
			@FloatRange(from = 0.0, to = XYZ_WHITE_REFERENCE_X) x: Double,
			@FloatRange(from = 0.0, to = XYZ_WHITE_REFERENCE_Y) y: Double,
			@FloatRange(from = 0.0, to = XYZ_WHITE_REFERENCE_Z) z: Double
		): Int {
			var r = (x * 3.2406 + y * -1.5372 + z * -0.4986) / 100
			var g = (x * -0.9689 + y * 1.8758 + z * 0.0415) / 100
			var b = (x * 0.0557 + y * -0.2040 + z * 1.0570) / 100
			r = if (r > 0.0031308) 1.055 * r.pow(1 / 2.4) - 0.055 else 12.92 * r
			g = if (g > 0.0031308) 1.055 * g.pow(1 / 2.4) - 0.055 else 12.92 * g
			b = if (b > 0.0031308) 1.055 * b.pow(1 / 2.4) - 0.055 else 12.92 * b
			return Color.rgb(
				(r * 255).roundToInt().coerceIn(0, 255),
				(g * 255).roundToInt().coerceIn(0, 255),
				(b * 255).roundToInt().coerceIn(0, 255),
			)
		}

		/**
		 * Converts a color from CIE Lab to its RGB representation.
		 *
		 * @param l L component value [0...100]
		 * @param a A component value [-128...127]
		 * @param b B component value [-128...127]
		 * @return int containing the RGB representation
		 */
		@ColorInt
		fun LABToColor(
			@FloatRange(from = 0.0, to = 100.0) l: Double,
			@FloatRange(from = -128.0, to = 127.0) a: Double,
			@FloatRange(from = -128.0, to = 127.0) b: Double
		): Int {
			val result = tempDouble3Array
			LABToXYZ(l, a, b, result)
			return XYZToColor(result[0], result[1], result[2])
		}

		private fun pivotXyzComponent(component: Double): Double {
			return if (component > XYZ_EPSILON) component.pow(1 / 3.0) else (XYZ_KAPPA * component + 16) / 116
		}

		val tempDouble3Array: DoubleArray
			get() {
				var result = TEMP_ARRAY.get()
				if (result == null) {
					result = DoubleArray(3)
					TEMP_ARRAY.set(result)
				}
				return result
			}

		/**
		 * Convert HSL (hue-saturation-lightness) components to a RGB color.
		 *
		 *  * hsl[0] is Hue [0 .. 360)
		 *  * hsl[1] is Saturation [0...1]
		 *  * hsl[2] is Lightness [0...1]
		 *
		 * If hsv values are out of range, they are pinned.
		 *
		 * @param hsl 3-element array which holds the input HSL components
		 * @return the resulting RGB color
		 */
		@ColorInt
		fun HSLToColor(hsl: FloatArray): Int {
			val h = hsl[0]
			val s = hsl[1]
			val l = hsl[2]
			val c = (1f - abs(2 * l - 1f)) * s
			val m = l - 0.5f * c
			val x = c * (1f - abs(h / 60f % 2f - 1f))
			val hueSegment = h.toInt() / 60
			var r = 0
			var g = 0
			var b = 0
			when (hueSegment) {
				0 -> {
					r = (255 * (c + m)).roundToInt()
					g = (255 * (x + m)).roundToInt()
					b = (255 * m).roundToInt()
				}
				1 -> {
					r = (255 * (x + m)).roundToInt()
					g = (255 * (c + m)).roundToInt()
					b = (255 * m).roundToInt()
				}
				2 -> {
					r = (255 * m).roundToInt()
					g = (255 * (c + m)).roundToInt()
					b = (255 * (x + m)).roundToInt()
				}
				3 -> {
					r = (255 * m).roundToInt()
					g = (255 * (x + m)).roundToInt()
					b = (255 * (c + m)).roundToInt()
				}
				4 -> {
					r = (255 * (x + m)).roundToInt()
					g = (255 * m).roundToInt()
					b = (255 * (c + m)).roundToInt()
				}
				5, 6 -> {
					r = (255 * (c + m)).roundToInt()
					g = (255 * m).roundToInt()
					b = (255 * (x + m)).roundToInt()
				}
			}
			r = r.coerceIn(0, 255)
			g = g.coerceIn(0, 255)
			b = b.coerceIn(0, 255)
			return Color.rgb(r, g, b)
		}

		/**
		 * Convert the ARGB color to its HSL (hue-saturation-lightness) components.
		 *
		 *  * outHsl[0] is Hue [0 .. 360)
		 *  * outHsl[1] is Saturation [0...1]
		 *  * outHsl[2] is Lightness [0...1]
		 *
		 *
		 * @param color  the ARGB color to convert. The alpha component is ignored
		 * @param outHsl 3-element array which holds the resulting HSL components
		 */
		fun colorToHSL(@ColorInt color: Int, outHsl: FloatArray) {
			RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), outHsl)
		}

		/**
		 * Convert RGB components to HSL (hue-saturation-lightness).
		 *
		 *  * outHsl[0] is Hue [0 .. 360)
		 *  * outHsl[1] is Saturation [0...1]
		 *  * outHsl[2] is Lightness [0...1]
		 *
		 *
		 * @param r      red component value [0..255]
		 * @param g      green component value [0..255]
		 * @param b      blue component value [0..255]
		 * @param outHsl 3-element array which holds the resulting HSL components
		 */
		fun RGBToHSL(
			@IntRange(from = 0x0, to = 0xFF) r: Int,
			@IntRange(from = 0x0, to = 0xFF) g: Int, @IntRange(from = 0x0, to = 0xFF) b: Int,
			outHsl: FloatArray
		) {
			val rf = r / 255f
			val gf = g / 255f
			val bf = b / 255f
			val max = rf.coerceAtLeast(gf.coerceAtLeast(bf))
			val min = rf.coerceAtMost(gf.coerceAtMost(bf))
			val deltaMaxMin = max - min
			var h: Float
			val s: Float
			val l = (max + min) / 2f
			if (max == min) {
				// Monochromatic
				s = 0f
				h = s
			} else {
				h = if (max == rf) {
					(gf - bf) / deltaMaxMin % 6f
				} else if (max == gf) {
					(bf - rf) / deltaMaxMin + 2f
				} else {
					(rf - gf) / deltaMaxMin + 4f
				}
				s = deltaMaxMin / (1f - abs(2f * l - 1f))
			}
			h = h * 60f % 360f
			if (h < 0) {
				h += 360f
			}
			outHsl[0] = h.coerceIn(0f, 360f)
			outHsl[1] = s.coerceIn(0f, 1f)
			outHsl[2] = l.coerceIn(0f, 1f)
		}
	}

	private const val TAG = "NotificationColorUtil"

	/**
	 * Finds a suitable color such that there's enough contrast.
	 *
	 * @param color the color to start searching from.
	 * @param other the color to ensure contrast against. Assumed to be lighter than {@param color}
	 * @param findFg if true, we assume {@param color} is a foreground, otherwise a background.
	 * @param minRatio the minimum contrast ratio required.
	 * @return a color with the same hue as {@param color}, potentially darkened to meet the
	 * contrast ratio.
	 */
	fun findContrastColor(color: Int, other: Int, findFg: Boolean, minRatio: Double): Int {
		var fg = if (findFg) color else other
		var bg = if (findFg) other else color
		if (ColorUtilsFromCompat.calculateContrast(fg, bg) >= minRatio) {
			return color
		}
		val lab = DoubleArray(3)
		ColorUtilsFromCompat.colorToLAB(if (findFg) fg else bg, lab)
		var low = 0.0
		var high = lab[0]
		val a = lab[1]
		val b = lab[2]
		var i = 0
		while (i < 15 && high - low > 0.00001) {
			val l = (low + high) / 2
			if (findFg) {
				fg = ColorUtilsFromCompat.LABToColor(l, a, b)
			} else {
				bg = ColorUtilsFromCompat.LABToColor(l, a, b)
			}
			if (ColorUtilsFromCompat.calculateContrast(fg, bg) > minRatio) {
				low = l
			} else {
				high = l
			}
			i++
		}
		return ColorUtilsFromCompat.LABToColor(low, a, b)
	}

	/**
	 * Finds a suitable color such that there's enough contrast.
	 *
	 * @param color the color to start searching from.
	 * @param other the color to ensure contrast against. Assumed to be darker than {@param color}
	 * @param findFg if true, we assume {@param color} is a foreground, otherwise a background.
	 * @param minRatio the minimum contrast ratio required.
	 * @return a color with the same hue as {@param color}, potentially darkened to meet the
	 * contrast ratio.
	 */
	fun findContrastColorAgainstDark(
		color: Int, other: Int, findFg: Boolean,
		minRatio: Double
	): Int {
		var fg = if (findFg) color else other
		var bg = if (findFg) other else color
		if (ColorUtilsFromCompat.calculateContrast(fg, bg) >= minRatio) {
			return color
		}
		val hsl = FloatArray(3)
		ColorUtilsFromCompat.colorToHSL(if (findFg) fg else bg, hsl)
		var low = hsl[2]
		var high = 1f
		var i = 0
		while (i < 15 && high - low > 0.00001) {
			val l = (low + high) / 2
			hsl[2] = l
			if (findFg) {
				fg = ColorUtilsFromCompat.HSLToColor(hsl)
			} else {
				bg = ColorUtilsFromCompat.HSLToColor(hsl)
			}
			if (ColorUtilsFromCompat.calculateContrast(fg, bg) > minRatio) {
				high = l
			} else {
				low = l
			}
			i++
		}
		return if (findFg) fg else bg
	}

	/**
	 * Change a color by a specified value
	 * @param baseColor the base color to lighten
	 * @param amount the amount to lighten the color from 0 to 100. This corresponds to the L
	 * increase in the LAB color space. A negative value will darken the color and
	 * a positive will lighten it.
	 * @return the changed color
	 */
	fun changeColorLightness(baseColor: Int, amount: Int): Int {
		val result = ColorUtilsFromCompat.tempDouble3Array
		ColorUtilsFromCompat.colorToLAB(baseColor, result)
		result[0] = 100.0.coerceAtMost(result[0] + amount).coerceAtLeast(0.0)
		return ColorUtilsFromCompat.LABToColor(result[0], result[1], result[2])
	}

	fun resolveActionBarColor(backgroundColor: Int): Int {
		return if (backgroundColor == Notification.COLOR_DEFAULT) {
			Color.BLACK
		} else getShiftedColor(backgroundColor, 7)
	}

	/**
	 * Get a color that stays in the same tint, but darkens or lightens it by a certain
	 * amount.
	 * This also looks at the lightness of the provided color and shifts it appropriately.
	 *
	 * @param color the base color to use
	 * @param amount the amount from 1 to 100 how much to modify the color
	 * @return the now color that was modified
	 */
	private fun getShiftedColor(color: Int, amount: Int): Int {
		val result = ColorUtilsFromCompat.tempDouble3Array
		ColorUtilsFromCompat.colorToLAB(color, result)
		if (result[0] >= 4) {
			result[0] = max(0.0, result[0] - amount)
		} else {
			result[0] = min(100.0, result[0] + amount)
		}
		return ColorUtilsFromCompat.LABToColor(result[0], result[1], result[2])
	}

	fun calculateLuminance(backgroundColor: Int): Double {
		return ColorUtilsFromCompat.calculateLuminance(backgroundColor)
	}

	fun calculateContrast(foregroundColor: Int, backgroundColor: Int): Double {
		return ColorUtilsFromCompat.calculateContrast(foregroundColor, backgroundColor)
	}

	fun satisfiesTextContrast(backgroundColor: Int, foregroundColor: Int): Boolean {
		return calculateContrast(foregroundColor, backgroundColor) >= 4.5
	}
}
