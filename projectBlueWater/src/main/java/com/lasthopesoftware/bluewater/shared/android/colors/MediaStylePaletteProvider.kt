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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.palette.graphics.Palette
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import kotlin.math.abs
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * A class the processes media notifications and extracts the right text and background colors.
 */
class MediaStylePaletteProvider(private val context: Context) : Palette.Filter {
	private fun promisePalette(drawable: Drawable): Promise<MediaStylePalette> =
		ThreadPools.compute.preparePromise { getMediaPalette(drawable) }

	override fun isAllowed(rgb: Int, hsl: FloatArray): Boolean = isNotWhiteOrBlack(hsl)

	fun promisePalette(bitmap: Bitmap): Promise<MediaStylePalette> {
		val drawable = bitmap.toDrawable(context.resources)
		return promisePalette(drawable)
	}

	/**
	 * Processes a drawable and calculates the appropriate colors that should
	 * be used.
	 */
	private fun getMediaPalette(drawable: Drawable): MediaStylePalette {
		// We're transforming the builder, let's make sure all baked in RemoteViews are
		// rebuilt!
		var width = drawable.intrinsicWidth
		var height = drawable.intrinsicHeight
		val area = width * height
		val maxBitmapArea = RESIZE_BITMAP_AREA.coerceAtMost(area)

		val factor = sqrt(maxBitmapArea.toDouble() / area)
		width = (factor * width).toInt()
		height = (factor * height).toInt()
		val bitmap: Bitmap = createBitmap(width, height)
		val canvas = Canvas(bitmap)
		drawable.setBounds(0, 0, width, height)
		drawable.draw(canvas)

		// for the background we only take the ~~left~~ bottom side of the image to ensure
		// a smooth transition
		val paletteBuilder = Palette.from(bitmap)
			.setRegion(0, bitmap.height / 2, bitmap.width, bitmap.height)
			.clearFilters() // we want all colors, red / white / black ones too!
			.resizeBitmapArea(maxBitmapArea)
		val backgroundColor = findBackgroundColorAndFilter(drawable, maxBitmapArea)
		// we want most of the full region again, slightly shifted to the right
//		val textColorStartWidthFraction = 0.4f
//		paletteBuilder.setRegion(
//			(bitmap.width * textColorStartWidthFraction).toInt(),
//			0,
//			bitmap.width,
//			bitmap.height
//		)

		if (backgroundColor.hsl != null) {
			paletteBuilder.addFilter { _, hsl ->
				// at least 10 degrees hue difference
				val diff: Float = abs(hsl[0] - backgroundColor.hsl[0])
				diff > 10 && diff < 350
			}
		}

		paletteBuilder.addFilter(this)
		val palette = paletteBuilder.generate()
		val foregroundColor = selectForegroundColor(backgroundColor.rgb, palette, maxBitmapArea)
		return finalizeColors(backgroundColor.rgb, foregroundColor)
	}

	private fun selectForegroundColor(backgroundColor: Int, palette: Palette, maxBitmapArea: Int): Int {
		return if (isColorLight(backgroundColor)) {
			selectForegroundColorForSwatches(
				palette.darkVibrantSwatch,
				palette.vibrantSwatch,
				palette.darkMutedSwatch,
				palette.mutedSwatch,
				palette.dominantSwatch,
				Color.BLACK,
				maxBitmapArea
			)
		} else {
			selectForegroundColorForSwatches(
				palette.lightVibrantSwatch,
				palette.vibrantSwatch,
				palette.lightMutedSwatch,
				palette.mutedSwatch,
				palette.dominantSwatch,
				Color.WHITE,
				maxBitmapArea
			)
		}
	}

	private fun selectForegroundColorForSwatches(
		moreVibrant: Palette.Swatch?,
		vibrant: Palette.Swatch?,
		moreMutedSwatch: Palette.Swatch?,
		mutedSwatch: Palette.Swatch?,
		dominantSwatch: Palette.Swatch?,
		fallbackColor: Int,
		maxBitmapArea: Int,
	): Int {
		val coloredCandidate =
			selectVibrantCandidate(moreVibrant, vibrant, maxBitmapArea) ?: selectMutedCandidate(mutedSwatch, moreMutedSwatch, maxBitmapArea)

		return if (coloredCandidate != null) {
			if (dominantSwatch === coloredCandidate || dominantSwatch == null) {
				coloredCandidate.rgb
			} else if (coloredCandidate.population.toFloat() / dominantSwatch.population.toFloat()
				< POPULATION_FRACTION_FOR_DOMINANT
				&& dominantSwatch.hsl[1] > MIN_SATURATION_WHEN_DECIDING
			) {
				dominantSwatch.rgb
			} else {
				coloredCandidate.rgb
			}
		} else if (dominantSwatch?.hasEnoughPopulation(maxBitmapArea) == true) {
			dominantSwatch.rgb
		} else {
			fallbackColor
		}
	}

	private fun selectMutedCandidate(first: Palette.Swatch?, second: Palette.Swatch?, maxBitmapArea: Int): Palette.Swatch? {
		val firstValid = first?.hasEnoughPopulation(maxBitmapArea) ?: return second
		val secondValid = second?.hasEnoughPopulation(maxBitmapArea) ?: return first
		return when {
			firstValid && secondValid -> {
				val firstSaturation = first.hsl[1]
				val secondSaturation = second.hsl[1]
				val populationFraction = first.population / second.population.toFloat()
				if (firstSaturation * populationFraction > secondSaturation) {
					first
				} else {
					second
				}
			}
			firstValid -> first
			secondValid -> second
			else -> null
		}
	}

	private fun selectVibrantCandidate(first: Palette.Swatch?, second: Palette.Swatch?, maxBitmapArea: Int): Palette.Swatch? {
		val firstValid = first?.hasEnoughPopulation(maxBitmapArea) ?: return second
		val secondValid = second?.hasEnoughPopulation(maxBitmapArea) ?: return first
		return when {
			firstValid && secondValid -> {
				val firstPopulation = first.population
				val secondPopulation = second.population
				if (firstPopulation / secondPopulation.toFloat() < POPULATION_FRACTION_FOR_MORE_VIBRANT) {
					second
				} else {
					first
				}
			}
			firstValid -> first
			secondValid -> second
			else -> null
		}
	}

	private fun Palette.Swatch.hasEnoughPopulation(maxBitmapArea: Int): Boolean =
		// We want a fraction that is at least 1% of the image
		population / maxBitmapArea > MINIMUM_IMAGE_FRACTION

	private fun findBackgroundColorAndFilter(drawable: Drawable, maxBitmapArea: Int): RgbAndHsl {
		var width: Int = drawable.intrinsicWidth
		var height: Int = drawable.intrinsicHeight
		val area = width * height
		val factor = sqrt(maxBitmapArea / area.toDouble())
		width = (factor * width).toInt()
		height = (factor * height).toInt()

		val bitmap = createBitmap(width, height)
		val canvas = Canvas(bitmap)
		drawable.setBounds(0, 0, width, height)
		drawable.draw(canvas)

		// for the background we only take the left side of the image to ensure
		// a smooth transition
		val paletteBuilder = Palette.from(bitmap)
			.setRegion(0, 0, bitmap.width, bitmap.height)
			.clearFilters() // we want all colors, red / white / black ones too!
			.resizeBitmapArea(RESIZE_BITMAP_AREA)

		val palette = paletteBuilder.generate()
		// by default we use the dominant palette
		val dominantSwatch = palette.dominantSwatch
			?: // We're not filtering on white or black
			return RgbAndHsl(Color.WHITE, null)

		if (isNotWhiteOrBlack(dominantSwatch.hsl)) {
			return RgbAndHsl(dominantSwatch.rgb, dominantSwatch.hsl)
		}

		// Oh well, we selected black or white. Lets look at the second color!
		val swatches: List<Palette.Swatch> = palette.swatches
		var highestNonWhitePopulation = -1f
		var second: Palette.Swatch? = null
		for (swatch in swatches) {
			if (swatch !== dominantSwatch && swatch.population > highestNonWhitePopulation && isNotWhiteOrBlack(swatch.hsl)) {
				second = swatch
				highestNonWhitePopulation = swatch.population.toFloat()
			}
		}

		if (second == null) {
			// We're not filtering on white or black
			return RgbAndHsl(dominantSwatch.rgb, null)
		}

		return if (dominantSwatch.population / highestNonWhitePopulation > POPULATION_FRACTION_FOR_WHITE_OR_BLACK) {
			// The dominant swatch is very dominant, lets take it!
			// We're not filtering on white or black
			RgbAndHsl(dominantSwatch.rgb, null)
		} else {
			RgbAndHsl(second.rgb, second.hsl)
		}
	}

	private fun isNotWhiteOrBlack(hsl: FloatArray): Boolean {
		return !isBlack(hsl) && !isWhite(hsl)
	}

	/**
	 * @return true if the color represents a color which is close to black.
	 */
	private fun isBlack(hslColor: FloatArray): Boolean {
		return hslColor[2] <= BLACK_MAX_LIGHTNESS
	}

	/**
	 * @return true if the color represents a color which is close to white.
	 */
	private fun isWhite(hslColor: FloatArray): Boolean {
		return hslColor[2] >= WHITE_MIN_LIGHTNESS
	}

	private fun finalizeColors(backgroundColor: Int, mForegroundColor: Int): MediaStylePalette {
		val backLum = NotificationColorUtil.calculateLuminance(backgroundColor)
		val textLum = NotificationColorUtil.calculateLuminance(mForegroundColor)
		val contrast = NotificationColorUtil.calculateContrast(
			mForegroundColor,
			backgroundColor
		)
		// We only respect the given colors if worst case Black or White still has
		// contrast
		val backgroundLight = (backLum > textLum
			&& NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.BLACK)
			|| backLum <= textLum
			&& !NotificationColorUtil.satisfiesTextContrast(backgroundColor, Color.WHITE))
		var secondaryTextColor: Int
		var primaryTextColor: Int
		if (contrast < 4.5f) {
			if (backgroundLight) {
				secondaryTextColor = NotificationColorUtil.findContrastColor(
					mForegroundColor,
					backgroundColor,
					true /* findFG */, 4.5
				)
				primaryTextColor = NotificationColorUtil.changeColorLightness(
					secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_LIGHT
				)
			} else {
				secondaryTextColor = NotificationColorUtil.findContrastColorAgainstDark(
					mForegroundColor,
					backgroundColor,
					true /* findFG */, 4.5
				)
				primaryTextColor = NotificationColorUtil.changeColorLightness(
					secondaryTextColor, -LIGHTNESS_TEXT_DIFFERENCE_DARK
				)
			}
		} else {
			primaryTextColor = mForegroundColor
			secondaryTextColor = NotificationColorUtil.changeColorLightness(
				primaryTextColor,
				if (backgroundLight) LIGHTNESS_TEXT_DIFFERENCE_LIGHT else LIGHTNESS_TEXT_DIFFERENCE_DARK
			)
			if (NotificationColorUtil.calculateContrast(
					secondaryTextColor,
					backgroundColor
				) < 4.5f
			) {
				// oh well the secondary is not good enough
				secondaryTextColor = if (backgroundLight) {
					NotificationColorUtil.findContrastColor(
						secondaryTextColor,
						backgroundColor,
						true /* findFG */, 4.5
					)
				} else {
					NotificationColorUtil.findContrastColorAgainstDark(
						secondaryTextColor,
						backgroundColor,
						true /* findFG */, 4.5
					)
				}
				primaryTextColor = NotificationColorUtil.changeColorLightness(
					secondaryTextColor,
					if (backgroundLight) -LIGHTNESS_TEXT_DIFFERENCE_LIGHT else -LIGHTNESS_TEXT_DIFFERENCE_DARK
				)
			}
		}

		return MediaStylePalette(
			ComposeColor(primaryTextColor),
			ComposeColor(secondaryTextColor),
			ComposeColor(backgroundColor),
			ComposeColor(NotificationColorUtil.resolveActionBarColor(backgroundColor))
		)
	}

	private class RgbAndHsl(val rgb: Int, val hsl: FloatArray?)
	companion object {
		/**
		 * The fraction below which we select the vibrant instead of the light/dark vibrant color
		 */
		private const val POPULATION_FRACTION_FOR_MORE_VIBRANT = 1.0f

		/**
		 * Minimum saturation that a muted color must have if there exists if deciding between two
		 * colors
		 */
		private const val MIN_SATURATION_WHEN_DECIDING = 0.19f

		/**
		 * Minimum fraction that any color must have to be picked up as a text color
		 */
		private const val MINIMUM_IMAGE_FRACTION = 0.002

		/**
		 * The population fraction to select the dominant color as the text color over a the colored
		 * ones.
		 */
		private const val POPULATION_FRACTION_FOR_DOMINANT = 0.01f

		/**
		 * The population fraction to select a white or black color as the background over a color.
		 */
		private const val POPULATION_FRACTION_FOR_WHITE_OR_BLACK = 2.5f
		private const val BLACK_MAX_LIGHTNESS = 0.08f
		private const val WHITE_MIN_LIGHTNESS = 0.90f
		private const val RESIZE_BITMAP_AREA = 150 * 150

		/**
		 * The lightness difference that has to be added to the primary text color to obtain the
		 * secondary text color when the background is light.
		 */
		private const val LIGHTNESS_TEXT_DIFFERENCE_LIGHT = 20

		/**
		 * The lightness difference that has to be added to the primary text color to obtain the
		 * secondary text color when the background is dark.
		 * A bit less then the above value, since it looks better on dark backgrounds.
		 */
		private const val LIGHTNESS_TEXT_DIFFERENCE_DARK = -10
		private fun isColorLight(backgroundColor: Int): Boolean {
			return calculateLuminance(backgroundColor) > 0.5f
		}

		/**
		 * Returns the luminance of a color as a float between `0.0` and `1.0`.
		 *
		 * Defined as the Y component in the XYZ representation of `color`.
		 */
		@FloatRange(from = 0.0, to = 1.0)
		private fun calculateLuminance(@ColorInt color: Int): Double {
			val result = DoubleArray(3)
			ColorUtils.colorToXYZ(color, result)
			// Luminance is the Y component
			return result[1] / 100
		}
	}
}
