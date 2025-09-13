package com.lasthopesoftware.bluewater.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.ScreenDimensionsScope
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.minimumMenuWidth

fun linearInterpolation(initial: Dp, final: Dp, progress: Float): Dp =
	initial + (final - initial) * progress

fun linearInterpolation(initial: Float, final: Float, progress: Float): Float =
	initial + (final - initial) * progress

fun calculateProgress(initial: Dp, final: Dp, currentPosition: Dp): Float =
	((currentPosition - initial) / (final - initial)).coerceIn(0f, 1f)

fun calculateProgress(initial: Float, final: Float, currentPosition: Float): Float =
	((currentPosition - initial) / (final - initial)).coerceIn(0f, 1f)

@Composable
inline fun <T> Density.remember(crossinline calculation: @DisallowComposableCalls Density.() -> T) =
	androidx.compose.runtime.remember(this) { this.calculation() }

@Composable
inline fun <T> Density.remember(key1: Any?, crossinline calculation: Density.() -> T) =
	androidx.compose.runtime.remember(this, key1) { this.calculation() }

@Composable
inline fun <T> Density.remember(key1: Any?, key2: Any?, crossinline calculation: Density.() -> T) =
	androidx.compose.runtime.remember(this, key1, key2) { this.calculation() }

@Composable
inline fun <T> Density.remember(key1: Any?, key2: Any?, key3: Any?, crossinline calculation: Density.() -> T) =
	androidx.compose.runtime.remember(this, key1, key2, key3) { this.calculation() }

@Composable
inline fun <T> Density.remember(key1: Any?, key2: Any?, key3: Any?, key4: Any?, crossinline calculation: Density.() -> T) =
	androidx.compose.runtime.remember(this, key1, key2, key3, key4) { this.calculation() }

@Composable
inline fun <T : AutoCloseable> rememberAutoCloseable(key1: Any?, key2: Any?, key3: Any?, crossinline calculation: () -> T): T {
	val result = androidx.compose.runtime.remember(key1, key2, key3, calculation)

	DisposableEffect(result) {
		onDispose { result.close() }
	}

	return result
}

/**
 * Calculates the width of the summary column based on the screen height, ensuring it stays within
 * the defined minimum menu width and half of the maximum screen width.
 *
 * This is used to determine an appropriate width for a side panel or menu that adapts to screen size
 * while maintaining usability.
 *
 * @receiver ScreenDimensionsScope Provides access to screen dimensions like screenHeight and maxWidth.
 * @return The calculated Dp value for the summary column width.
 */
fun ScreenDimensionsScope.calculateSummaryColumnWidth() =
	screenHeight.coerceIn(minimumMenuWidth, maxWidth / 2)
