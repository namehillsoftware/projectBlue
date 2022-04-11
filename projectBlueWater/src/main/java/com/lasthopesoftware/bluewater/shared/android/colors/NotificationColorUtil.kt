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
import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Helper class to process legacy (Holo) notifications to make them look like material notifications.
 *
 */
class NotificationColorUtil private constructor() {

	/**
	 * Framework copy of functions needed from android.support.v4.graphics.ColorUtils.
	 */
	private object ColorUtilsFromCompat {
		private const val XYZ_WHITE_REFERENCE_X = 95.047
		private const val XYZ_WHITE_REFERENCE_Y = 100.0
		private const val XYZ_WHITE_REFERENCE_Z = 108.883
		private const val XYZ_EPSILON = 0.008856
		private const val XYZ_KAPPA = 903.3
		private const val MIN_ALPHA_SEARCH_MAX_ITERATIONS = 10
		private const val MIN_ALPHA_SEARCH_PRECISION = 1
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
			var foreground = foreground
			if (Color.alpha(background) != 255) {
				Log.wtf(
					TAG, "background can not be translucent: #"
						+ Integer.toHexString(background)
				)
			}
			if (Color.alpha(foreground) < 255) {
				// If the foreground is translucent, composite the foreground over the background
				foreground = compositeColors(foreground, background)
			}
			val luminance1 = calculateLuminance(foreground) + 0.05
			val luminance2 = calculateLuminance(background) + 0.05

			// Now return the lighter luminance divided by the darker luminance
			return Math.max(luminance1, luminance2) / Math.min(luminance1, luminance2)
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
			sr = if (sr < 0.04045) sr / 12.92 else Math.pow((sr + 0.055) / 1.055, 2.4)
			var sg = g / 255.0
			sg = if (sg < 0.04045) sg / 12.92 else Math.pow((sg + 0.055) / 1.055, 2.4)
			var sb = b / 255.0
			sb = if (sb < 0.04045) sb / 12.92 else Math.pow((sb + 0.055) / 1.055, 2.4)
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
			var x = x
			var y = y
			var z = z
			require(outLab.size == 3) { "outLab must have a length of 3." }
			x = pivotXyzComponent(x / XYZ_WHITE_REFERENCE_X)
			y = pivotXyzComponent(y / XYZ_WHITE_REFERENCE_Y)
			z = pivotXyzComponent(z / XYZ_WHITE_REFERENCE_Z)
			outLab[0] = 0.0.coerceAtLeast(116 * y - 16)
			outLab[1] = 500 * (x - y)
			outLab[2] = 200 * (y - z)
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
			var tmp = Math.pow(fx, 3.0)
			val xr = if (tmp > XYZ_EPSILON) tmp else (116 * fx - 16) / XYZ_KAPPA
			val yr = if (l > XYZ_KAPPA * XYZ_EPSILON) Math.pow(fy, 3.0) else l / XYZ_KAPPA
			tmp = Math.pow(fz, 3.0)
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
				constrain((r * 255).roundToInt(), 0, 255),
				constrain((g * 255).roundToInt(), 0, 255),
				constrain((b * 255).roundToInt(), 0, 255)
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

		private fun constrain(amount: Int, low: Int, high: Int): Int {
			return if (amount < low) low else if (amount > high) high else amount
		}

		private fun constrain(amount: Float, low: Float, high: Float): Float {
			return if (amount < low) low else if (amount > high) high else amount
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
			val c = (1f - Math.abs(2 * l - 1f)) * s
			val m = l - 0.5f * c
			val x = c * (1f - Math.abs(h / 60f % 2f - 1f))
			val hueSegment = h.toInt() / 60
			var r = 0
			var g = 0
			var b = 0
			when (hueSegment) {
				0 -> {
					r = Math.round(255 * (c + m))
					g = Math.round(255 * (x + m))
					b = Math.round(255 * m)
				}
				1 -> {
					r = Math.round(255 * (x + m))
					g = Math.round(255 * (c + m))
					b = Math.round(255 * m)
				}
				2 -> {
					r = Math.round(255 * m)
					g = Math.round(255 * (c + m))
					b = Math.round(255 * (x + m))
				}
				3 -> {
					r = Math.round(255 * m)
					g = Math.round(255 * (x + m))
					b = Math.round(255 * (c + m))
				}
				4 -> {
					r = Math.round(255 * (x + m))
					g = Math.round(255 * m)
					b = Math.round(255 * (c + m))
				}
				5, 6 -> {
					r = Math.round(255 * (c + m))
					g = Math.round(255 * m)
					b = Math.round(255 * (x + m))
				}
			}
			r = constrain(r, 0, 255)
			g = constrain(g, 0, 255)
			b = constrain(b, 0, 255)
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
			outHsl[0] = constrain(h, 0f, 360f)
			outHsl[1] = constrain(s, 0f, 1f)
			outHsl[2] = constrain(l, 0f, 1f)
		}
	}

	companion object {
		private const val TAG = "NotificationColorUtil"
		private const val DEBUG = false
		private val sLock = Any()
		private var sInstance: NotificationColorUtil? = null
		fun getInstance(context: Context): NotificationColorUtil? {
			synchronized(sLock) {
				if (sInstance == null) {
					sInstance = NotificationColorUtil()
				}
				return sInstance
			}
		}
		//    /**
		//     * Inverts all the grayscale colors set by {@link android.text.style.TextAppearanceSpan}s on
		//     * the text.
		//     *
		//     * @param charSequence The text to process.
		//     * @return The color inverted text.
		//     */
		//    public CharSequence invertCharSequenceColors(CharSequence charSequence) {
		//        if (charSequence instanceof Spanned) {
		//            Spanned ss = (Spanned) charSequence;
		//            Object[] spans = ss.getSpans(0, ss.length(), Object.class);
		//            SpannableStringBuilder builder = new SpannableStringBuilder(ss.toString());
		//            for (Object span : spans) {
		//                Object resultSpan = span;
		//                if (resultSpan instanceof CharacterStyle) {
		//                    resultSpan = ((CharacterStyle) span).getUnderlying();
		//                }
		//                if (resultSpan instanceof TextAppearanceSpan) {
		//                    TextAppearanceSpan processedSpan = processTextAppearanceSpan(
		//                            (TextAppearanceSpan) span);
		//                    if (processedSpan != resultSpan) {
		//                        resultSpan = processedSpan;
		//                    } else {
		//                        // we need to still take the orgininal for wrapped spans
		//                        resultSpan = span;
		//                    }
		//                } else if (resultSpan instanceof ForegroundColorSpan) {
		//                    ForegroundColorSpan originalSpan = (ForegroundColorSpan) resultSpan;
		//                    int foregroundColor = originalSpan.getForegroundColor();
		//                    resultSpan = new ForegroundColorSpan(processColor(foregroundColor));
		//                } else {
		//                    resultSpan = span;
		//                }
		//                builder.setSpan(resultSpan, ss.getSpanStart(span), ss.getSpanEnd(span),
		//                        ss.getSpanFlags(span));
		//            }
		//            return builder;
		//        }
		//        return charSequence;
		//    }
		//    private TextAppearanceSpan processTextAppearanceSpan(TextAppearanceSpan span) {
		//        ColorStateList colorStateList = span.getTextColor();
		//        if (colorStateList != null) {
		//            int[] colors = colorStateList.getColors();
		//            boolean changed = false;
		//            for (int i = 0; i < colors.length; i++) {
		//                if (ImageUtils.isGrayscale(colors[i])) {
		//
		//                    // Allocate a new array so we don't change the colors in the old color state
		//                    // list.
		//                    if (!changed) {
		//                        colors = Arrays.copyOf(colors, colors.length);
		//                    }
		//                    colors[i] = processColor(colors[i]);
		//                    changed = true;
		//                }
		//            }
		//            if (changed) {
		//                return new TextAppearanceSpan(
		//                        span.getFamily(), span.getTextStyle(), span.getTextSize(),
		//                        new ColorStateList(colorStateList.getStates(), colors),
		//                        span.getLinkTextColor());
		//            }
		//        }
		//        return span;
		//    }
		/**
		 * Clears all color spans of a text
		 * @param charSequence the input text
		 * @return the same text but without color spans
		 */
		fun clearColorSpans(charSequence: CharSequence): CharSequence {
			if (charSequence is Spanned) {
				val spans = charSequence.getSpans(0, charSequence.length, Any::class.java)
				val builder = SpannableStringBuilder(charSequence.toString())
				for (span in spans) {
					var resultSpan = span
					if (resultSpan is CharacterStyle) {
						resultSpan = (span as CharacterStyle).underlying
					}
					if (resultSpan is TextAppearanceSpan) {
						val originalSpan = resultSpan
						if (originalSpan.textColor != null) {
							resultSpan = TextAppearanceSpan(
								originalSpan.family,
								originalSpan.textStyle,
								originalSpan.textSize,
								null,
								originalSpan.linkTextColor
							)
						}
					} else if (resultSpan is ForegroundColorSpan
						|| resultSpan is BackgroundColorSpan
					) {
						continue
					} else {
						resultSpan = span
					}
					builder.setSpan(
						resultSpan, charSequence.getSpanStart(span), charSequence.getSpanEnd(span),
						charSequence.getSpanFlags(span)
					)
				}
				return builder
			}
			return charSequence
		}

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
		 * Finds a suitable alpha such that there's enough contrast.
		 *
		 * @param color the color to start searching from.
		 * @param backgroundColor the color to ensure contrast against.
		 * @param minRatio the minimum contrast ratio required.
		 * @return the same color as {@param color} with potentially modified alpha to meet contrast
		 */
		fun findAlphaToMeetContrast(color: Int, backgroundColor: Int, minRatio: Double): Int {
			var fg = color
			if (ColorUtilsFromCompat.calculateContrast(fg, backgroundColor) >= minRatio) {
				return color
			}
			val startAlpha = Color.alpha(color)
			val r = Color.red(color)
			val g = Color.green(color)
			val b = Color.blue(color)
			var low = startAlpha
			var high = 255
			var i = 0
			while (i < 15 && high - low > 0) {
				val alpha = (low + high) / 2
				fg = Color.argb(alpha, r, g, b)
				if (ColorUtilsFromCompat.calculateContrast(fg, backgroundColor) > minRatio) {
					high = alpha
				} else {
					low = alpha
				}
				i++
			}
			return Color.argb(high, r, g, b)
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

		fun ensureTextContrastOnBlack(color: Int): Int {
			return findContrastColorAgainstDark(color, Color.BLACK, true /* fg */, 12.0)
		}

		/**
		 * Finds a large text color with sufficient contrast over bg that has the same or darker hue as
		 * the original color, depending on the value of `isBgDarker`.
		 *
		 * @param isBgDarker `true` if `bg` is darker than `color`.
		 */
		fun ensureLargeTextContrast(color: Int, bg: Int, isBgDarker: Boolean): Int {
			return if (isBgDarker) findContrastColorAgainstDark(
				color,
				bg,
				true,
				3.0
			) else findContrastColor(color, bg, true, 3.0)
		}

		/**
		 * Finds a text color with sufficient contrast over bg that has the same or darker hue as the
		 * original color, depending on the value of `isBgDarker`.
		 *
		 * @param isBgDarker `true` if `bg` is darker than `color`.
		 */
		private fun ensureTextContrast(color: Int, bg: Int, isBgDarker: Boolean): Int {
			return if (isBgDarker) findContrastColorAgainstDark(
				color,
				bg,
				true,
				4.5
			) else findContrastColor(color, bg, true, 4.5)
		}

		/** Finds a background color for a text view with given text color and hint text color, that
		 * has the same hue as the original color.
		 */
		fun ensureTextBackgroundColor(color: Int, textColor: Int, hintColor: Int): Int {
			var color = color
			color = findContrastColor(color, hintColor, false, 3.0)
			return findContrastColor(color, textColor, false, 4.5)
		}

		private fun contrastChange(colorOld: Int, colorNew: Int, bg: Int): String {
			return String.format(
				"from %.2f:1 to %.2f:1",
				ColorUtilsFromCompat.calculateContrast(colorOld, bg),
				ColorUtilsFromCompat.calculateContrast(colorNew, bg)
			)
		}
		//    /**
		//     * Resolves {@param color} to an actual color if it is {@link Notification#COLOR_DEFAULT}
		//     */
		//    public static int resolveColor(Context context, int color) {
		//        if (color == Notification.COLOR_DEFAULT) {
		//            return context.getColor(com.android.internal.R.color.notification_icon_default_color);
		//        }
		//        return color;
		//    }
		//
		//
		//    public static int resolveContrastColor(Context context, int notificationColor,
		//                                           int backgroundColor) {
		//        return NotificationColorUtil.resolveContrastColor(context, notificationColor,
		//                backgroundColor, false /* isDark */);
		//    }
		//    /**
		//     * Resolves a Notification's color such that it has enough contrast to be used as the
		//     * color for the Notification's action and header text.
		//     *
		//     * @param notificationColor the color of the notification or {@link Notification#COLOR_DEFAULT}
		//     * @param backgroundColor the background color to ensure the contrast against.
		//     * @param isDark whether or not the {@code notificationColor} will be placed on a background
		//     *               that is darker than the color itself
		//     * @return a color of the same hue with enough contrast against the backgrounds.
		//     */
		//    public static int resolveContrastColor(Context context, int notificationColor,
		//                                           int backgroundColor, boolean isDark) {
		//        final int resolvedColor = resolveColor(context, notificationColor);
		//
		//        final int actionBg = context.getColor(
		//                com.android.internal.R.color.notification_action_list);
		//
		//        int color = resolvedColor;
		//        color = NotificationColorUtil.ensureLargeTextContrast(color, actionBg, isDark);
		//        color = NotificationColorUtil.ensureTextContrast(color, backgroundColor, isDark);
		//
		//        if (color != resolvedColor) {
		//            if (DEBUG){
		//                Log.w(TAG, String.format(
		//                        "Enhanced contrast of notification for %s %s (over action)"
		//                                + " and %s (over background) by changing #%s to %s",
		//                        context.getPackageName(),
		//                        NotificationColorUtil.contrastChange(resolvedColor, color, actionBg),
		//                        NotificationColorUtil.contrastChange(resolvedColor, color, backgroundColor),
		//                        Integer.toHexString(resolvedColor), Integer.toHexString(color)));
		//            }
		//        }
		//        return color;
		//    }
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

		//    public static int resolveAmbientColor(Context context, int notificationColor) {
		//        final int resolvedColor = resolveColor(context, notificationColor);
		//
		//        int color = resolvedColor;
		//        color = NotificationColorUtil.ensureTextContrastOnBlack(color);
		//
		//        if (color != resolvedColor) {
		//            if (DEBUG){
		//                Log.w(TAG, String.format(
		//                        "Ambient contrast of notification for %s is %s (over black)"
		//                                + " by changing #%s to #%s",
		//                        context.getPackageName(),
		//                        NotificationColorUtil.contrastChange(resolvedColor, color, Color.BLACK),
		//                        Integer.toHexString(resolvedColor), Integer.toHexString(color)));
		//            }
		//        }
		//        return color;
		//    }
		//    public static int resolvePrimaryColor(Context context, int backgroundColor) {
		//        boolean useDark = shouldUseDark(backgroundColor);
		//        if (useDark) {
		//            return context.getColor(
		//                    com.android.internal.R.color.notification_primary_text_color_light);
		//        } else {
		//            return context.getColor(
		//                    com.android.internal.R.color.notification_primary_text_color_dark);
		//        }
		//    }
		//
		//    public static int resolveSecondaryColor(Context context, int backgroundColor) {
		//        boolean useDark = shouldUseDark(backgroundColor);
		//        if (useDark) {
		//            return context.getColor(
		//                    com.android.internal.R.color.notification_secondary_text_color_light);
		//        } else {
		//            return context.getColor(
		//                    com.android.internal.R.color.notification_secondary_text_color_dark);
		//        }
		//    }
		//
		fun resolveActionBarColor(context: Context?, backgroundColor: Int): Int {
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
		fun getShiftedColor(color: Int, amount: Int): Int {
			val result = ColorUtilsFromCompat.tempDouble3Array
			ColorUtilsFromCompat.colorToLAB(color, result)
			if (result[0] >= 4) {
				result[0] = Math.max(0.0, result[0] - amount)
			} else {
				result[0] = Math.min(100.0, result[0] + amount)
			}
			return ColorUtilsFromCompat.LABToColor(result[0], result[1], result[2])
		}

		private fun shouldUseDark(backgroundColor: Int): Boolean {
			var useDark = backgroundColor == Notification.COLOR_DEFAULT
			if (!useDark) {
				useDark = ColorUtilsFromCompat.calculateLuminance(backgroundColor) > 0.5
			}
			return useDark
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

		/**
		 * Composite two potentially translucent colors over each other and returns the result.
		 */
		fun compositeColors(foreground: Int, background: Int): Int {
			return ColorUtilsFromCompat.compositeColors(foreground, background)
		}

		fun isColorLight(backgroundColor: Int): Boolean {
			return calculateLuminance(backgroundColor) > 0.5f
		}
	}

}
