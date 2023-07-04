package com.lasthopesoftware.bluewater.shared.android.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

private const val logTag = "ScrollConnectedScaler"

/**
 * Will scale a value based on a nested scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun memorableScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = ScrollConnectedScalerSaver(max, min)) {
	ScrollConnectedScaler(max, min)
}

class ScrollConnectedScalerSaver(private val max: Float, private val min: Float) : Saver<ScrollConnectedScaler, Float> {
	override fun restore(value: Float): ScrollConnectedScaler =
		ScrollConnectedScaler(max, min, value)

	override fun SaverScope.save(value: ScrollConnectedScaler): Float = value.progress
}

class ScrollConnectedScaler(private val max: Float, private val min: Float, initialProgress: Float = 0f) : NestedScrollConnection {

	private val fullDistance = max - min

	var value by mutableStateOf(max - (fullDistance * initialProgress))
		private set

	var progress by mutableStateOf(initialProgress)
		private set

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		val newOffset = value + delta
		val originalValue = value
		value = newOffset.coerceIn(min, max)
		progress = (max - value) / fullDistance

		Log.d(logTag, "height: $value, progress: $progress")

		// here's the catch: let's pretend we consumed 0 in any case, since we want
		// LazyColumn to scroll anyway for good UX
		// We're basically watching scroll without taking it
		return Offset.Zero.copy(y = value - originalValue)
	}

	fun goToMax() {
		value = max
		progress = 1f
	}

	fun goToMin() {
		value = min
		progress = 0f
	}
}
