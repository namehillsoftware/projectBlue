package com.lasthopesoftware.bluewater.shared.android.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

private const val logTag = "ScrollConnectedScaler"

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun memorableScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = ScrollConnectedScalerSaver(max, min)) {
	ScrollConnectedScaler(max, min)
}

class ScrollConnectedScalerSaver(private val max: Float, private val min: Float) : Saver<ScrollConnectedScaler, Float> {
	override fun restore(value: Float): ScrollConnectedScaler =
		ScrollConnectedScaler(max, min, value)

	override fun SaverScope.save(value: ScrollConnectedScaler): Float = value.state.progress
}

class ScrollConnectedScaler(private val max: Float, private val min: Float, initialProgress: Float = 0f) : NestedScrollConnection {

	private val fullDistance = max - min

	var state by mutableStateOf(ScrollConnectedScalerState(max - (fullDistance * initialProgress), initialProgress))
		private set

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		val originalValue = state.value
		val newOffset = originalValue + delta
		val newValue = newOffset.coerceIn(min, max)

		state = ScrollConnectedScalerState(newValue, (max - newValue) / fullDistance)

		Log.d(logTag, "state: $state")

		// here's the catch: let's pretend we consumed 0 in any case, since we want
		// LazyColumn to scroll anyway for good UX
		// We're basically watching scroll without taking it
		return Offset.Zero.copy(y = newValue - originalValue)
	}

	@Composable
	fun rememberProgress() = remember {
		derivedStateOf { state.progress }
	}

	@Composable
	fun rememberValue() = remember {
		derivedStateOf { state.value }
	}

	fun goToMax() {
		state = ScrollConnectedScalerState(max, 1f)
	}

	fun goToMin() {
		state = ScrollConnectedScalerState(min, 0f)
	}
}

data class ScrollConnectedScalerState(val value: Float, val progress: Float)
