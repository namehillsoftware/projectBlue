package com.lasthopesoftware.bluewater.android.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.lasthopesoftware.compilation.DebugFlag

private const val logTag = "ScrollConnectedScaler"

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun memorableScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = ScrollConnectedScaler.Saver) {
	ScrollConnectedScaler(max, min)
}

class ScrollConnectedScaler private constructor(private val max: Float, private val min: Float, initialDistanceTraveled: Float) : NestedScrollConnection {

	constructor(max: Float, min: Float): this(max, min, 0f)

	private val fullDistance = max - min

	private val totalDistanceTraveled = mutableFloatStateOf(initialDistanceTraveled)

	private val valueState = derivedStateOf { (max + totalDistanceTraveled.floatValue).coerceIn(min, max) }

	private val progress = derivedStateOf { calculateProgress(valueState.value) }

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

	fun getProgressState(): State<Float> = progress

	fun getValueState(): State<Float> = valueState

	fun goToMax() {
		totalDistanceTraveled.floatValue = 0f
	}

	fun goToMin() {
		totalDistanceTraveled.floatValue = -fullDistance
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else (max - value) / fullDistance

	object Saver : androidx.compose.runtime.saveable.Saver<ScrollConnectedScaler, Triple<Float, Float, Float>> {
		override fun restore(value: Triple<Float, Float, Float>): ScrollConnectedScaler =
			ScrollConnectedScaler(value.first, value.second, value.third)

		override fun SaverScope.save(value: ScrollConnectedScaler): Triple<Float, Float, Float> =
			Triple(value.max, value.min, value.totalDistanceTraveled.floatValue)
	}
}
