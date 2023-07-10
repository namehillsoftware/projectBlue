package com.lasthopesoftware.bluewater.shared.android.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.lasthopesoftware.compilation.DebugFlag

private const val logTag = "ScrollTravelDistance"

/**
 * Tracks the unconsumed travel distance of a scroller
 */
@Composable
fun rememberScrollTravelDistance() = rememberSaveable(saver = ScrollTravelDistanceMonitor.Saver) {
	ScrollTravelDistanceMonitor()
}

class ScrollTravelDistanceMonitor private constructor(initialDistanceTraveled: Float) : NestedScrollConnection {

	constructor(): this(0f)

	private val mutableTotalDistanceTraveled = mutableStateOf(initialDistanceTraveled)

	val totalDistanceTraveled: State<Float> = mutableTotalDistanceTraveled

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		mutableTotalDistanceTraveled.value += delta

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${mutableTotalDistanceTraveled.value}")
		}

		return Offset.Zero
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		mutableTotalDistanceTraveled.value -= available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${mutableTotalDistanceTraveled.value}")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return super.onPostScroll(consumed, available, source)
	}

	object Saver : androidx.compose.runtime.saveable.Saver<ScrollTravelDistanceMonitor, Float> {
		override fun restore(value: Float): ScrollTravelDistanceMonitor =
			ScrollTravelDistanceMonitor(value)

		override fun SaverScope.save(value: ScrollTravelDistanceMonitor): Float =
			value.mutableTotalDistanceTraveled.value
	}
}

fun State<Float>.boundedValue(min: Float, max: Float): State<Float> {
	check(min < max)

	return derivedStateOf { (min + value).coerceIn(min, max) }
}

fun State<Float>.progress(min: Float, max: Float): State<Float> {
	check(min < max)

	val totalDistance = max - min
	return derivedStateOf { ((min + value).coerceIn(min, max) / totalDistance).coerceIn(0f, 1f) }
}
