package com.lasthopesoftware.bluewater.shared.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp

fun linearInterpolation(initial: Dp, final: Dp, progress: Float): Dp =
	initial + (final - initial) * progress

fun linearInterpolation(initial: Float, final: Float, progress: Float): Float =
	initial + (final - initial) * progress

fun calculateProgress(initial: Float, final: Float, currentPosition: Float): Float =
	((currentPosition - initial) / (final - initial)).coerceIn(0f, 1f)

@OptIn(ExperimentalFoundationApi::class)
val <T> AnchoredDraggableState<T>.absoluteProgressState: State<Float>
	@Composable
	get() = remember { derivedStateOf { (requireOffset() - anchors.minAnchor()) / anchors.maxAnchor() } }
