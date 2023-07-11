package com.lasthopesoftware.bluewater.shared.android.ui

import androidx.compose.ui.unit.Dp

fun linearInterpolation(initial: Dp, final: Dp, progress: Float): Dp =
	initial + (final - initial) * progress

fun linearInterpolation(initial: Float, final: Float, progress: Float): Float =
	initial + (final - initial) * progress
