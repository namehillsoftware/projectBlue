package com.lasthopesoftware.bluewater.shared.android.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.lasthopesoftware.compilation.DebugFlag
import io.reactivex.subjects.BehaviorSubject

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

	private val initialValue = (max + initialDistanceTraveled).coerceIn(min, max)

	private val valueState = BehaviorSubject.createDefault(initialValue)

	private val progress = valueState
		.map(::calculateProgress)
		.replay(1)
		.refCount()

	val fullDistance = max - min

	var totalDistanceTraveled = initialDistanceTraveled
		private set

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		totalDistanceTraveled += delta
		val newValue = (max + totalDistanceTraveled).coerceIn(min, max)

		val originalValue = currentValue()
		val remainingDelta = newValue - originalValue
		if (remainingDelta != 0f)
			valueState.onNext(newValue)

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "newValue: $newValue")
		}

		return available.copy(y = remainingDelta)
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		// Only observe if some scroll was left available
		if (available.y != 0f)
			totalDistanceTraveled -= (consumed.y + available.y)

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return super.onPostScroll(consumed, available, source)
	}

	@Composable
	fun rememberProgress() = progress.subscribeAsState(initial = calculateProgress(currentValue()))

	@Composable
	fun rememberValue() = valueState.subscribeAsState(initial = currentValue())

	fun goToMax() {
		valueState.onNext(max)
	}

	fun goToMin() {
		valueState.onNext(min)
	}

	private fun currentValue(): Float = valueState.value ?: initialValue

	private fun calculateProgress(value: Float) = (max - value) / fullDistance

	object Saver : androidx.compose.runtime.saveable.Saver<ScrollConnectedScaler, Triple<Float, Float, Float>> {
		override fun restore(value: Triple<Float, Float, Float>): ScrollConnectedScaler =
			ScrollConnectedScaler(value.first, value.second, value.third)

		override fun SaverScope.save(value: ScrollConnectedScaler): Triple<Float, Float, Float> =
			Triple(value.max, value.min, value.totalDistanceTraveled)
	}
}
