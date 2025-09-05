package com.lasthopesoftware.bluewater.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

fun linearInterpolation(initial: Dp, final: Dp, progress: Float): Dp =
	initial + (final - initial) * progress

fun linearInterpolation(initial: Float, final: Float, progress: Float): Float =
	initial + (final - initial) * progress

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
