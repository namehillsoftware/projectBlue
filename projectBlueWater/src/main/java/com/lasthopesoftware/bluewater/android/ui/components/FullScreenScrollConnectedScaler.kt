package com.lasthopesoftware.bluewater.android.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.lasthopesoftware.compilation.DebugFlag

private const val logTag = "ScrollConnectedScaler"

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun memorableFullScreenScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = FullScreenScrollConnectedScaler.Saver) {
	FullScreenScrollConnectedScaler(max, min)
}

class FullScreenScrollConnectedScaler private constructor(private val max: Float, private val min: Float, initialDistanceTraveled: Float) : NestedScrollConnection {

	constructor(max: Float, min: Float): this(max, min, 0f)

	private val fullDistance = max - min

	private val totalDistanceTraveled = mutableFloatStateOf(initialDistanceTraveled)

	val valueState by lazy { derivedStateOf { (max + totalDistanceTraveled.floatValue).coerceIn(min, max) } }

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		totalDistanceTraveled.floatValue += delta

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${totalDistanceTraveled.floatValue}")
			Log.d(logTag, "valueState.value: ${valueState.value}")
		}

		return Offset.Zero
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		totalDistanceTraveled.floatValue -= available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${totalDistanceTraveled.floatValue}")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return super.onPostScroll(consumed, available, source)
	}

	fun goToMax() {
		totalDistanceTraveled.floatValue = 0f
	}

	fun goToMin() {
		totalDistanceTraveled.floatValue = -fullDistance
	}

	fun overrideDistanceTraveled(distance: Float) {
		totalDistanceTraveled.floatValue = distance
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else (max - value) / fullDistance

	object Saver : androidx.compose.runtime.saveable.Saver<FullScreenScrollConnectedScaler, Triple<Float, Float, Float>> {
		override fun restore(value: Triple<Float, Float, Float>): FullScreenScrollConnectedScaler =
			FullScreenScrollConnectedScaler(value.first, value.second, value.third)

		override fun SaverScope.save(value: FullScreenScrollConnectedScaler): Triple<Float, Float, Float> =
			Triple(value.max, value.min, value.totalDistanceTraveled.floatValue)
	}
}

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun memorableScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = ScrollConnectedScaler.Saver) {
	ScrollConnectedScaler(max, min)
}

class ScrollConnectedScaler private constructor(private val max: Float, private val min: Float, initialDistanceTraveled: Float) : NestedScrollConnection {

	constructor(max: Float, min: Float): this(max, min, max)

	private val fullDistance = max - min

	private val mutableValueState = mutableFloatStateOf(initialDistanceTraveled)

	val valueState = mutableValueState.asFloatState()

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		val newOffset = mutableValueState.floatValue + delta
		mutableValueState.floatValue = newOffset.coerceIn(min, max)

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "valueState.value: ${valueState.value}")
		}

		// here's the catch: let's pretend we consumed 0 in any case, since we want
		// LazyColumn to scroll anyway for good UX
		// We're basically watching scroll without taking it
		return Offset.Zero
	}

	fun goToMax() {
		mutableValueState.floatValue = 0f
	}

	fun goToMin() {
		mutableValueState.floatValue = -fullDistance
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else (max - value) / fullDistance

	object Saver : androidx.compose.runtime.saveable.Saver<ScrollConnectedScaler, Triple<Float, Float, Float>> {
		override fun restore(value: Triple<Float, Float, Float>): ScrollConnectedScaler =
			ScrollConnectedScaler(value.first, value.second, value.third)

		override fun SaverScope.save(value: ScrollConnectedScaler): Triple<Float, Float, Float> =
			Triple(value.max, value.min, value.valueState.floatValue)
	}
}

class LinkedNestedScrollConnection(
	private val first: NestedScrollConnection,
	private val second: NestedScrollConnection
) : NestedScrollConnection {
	override suspend fun onPreFling(available: Velocity): Velocity {
		val nextAvailable = first.onPreFling(available)
		return second.onPreFling(nextAvailable)
	}

	override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
		val nextConsumed = first.onPostFling(consumed, available)
		return second.onPostFling(consumed + nextConsumed, available - nextConsumed)
	}

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val consumed = first.onPreScroll(available, source)
		return second.onPreScroll(available - consumed, source)
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		val nextConsumed = first.onPostScroll(consumed, available, source)
		return second.onPostScroll(consumed + nextConsumed, available - nextConsumed, source)
	}
}
