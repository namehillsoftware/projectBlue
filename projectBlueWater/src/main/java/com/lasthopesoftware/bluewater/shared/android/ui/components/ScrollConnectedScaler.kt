package com.lasthopesoftware.bluewater.shared.android.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import io.reactivex.subjects.BehaviorSubject

private const val logTag = "ScrollConnectedScaler"

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun memorableScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = ScrollConnectedScaler(max, min)) {
	ScrollConnectedScaler(max, min)
}

class ScrollConnectedScaler private constructor(private val max: Float, private val min: Float, initialDistanceTraveled: Float) : NestedScrollConnection, Saver<ScrollConnectedScaler, Float> {

	constructor(max: Float, min: Float): this(max, min, 0f)

	private val fullDistance = max - min

	private var totalDistanceTraveled = initialDistanceTraveled

	private val initialValue = (max + totalDistanceTraveled).coerceIn(min, max)

	private val valueState = BehaviorSubject.createDefault(initialValue)

	private val progress = valueState
		.map(::calculateProgress)
		.replay(1)
		.refCount()

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		totalDistanceTraveled = totalDistanceTraveled.plus(delta).coerceAtMost(0f)
		val newValue = (max + totalDistanceTraveled).coerceIn(min, max)

		val originalValue = currentValue()
		if (newValue != originalValue)
			valueState.onNext(newValue)

		Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
		Log.d(logTag, "newValue: $newValue")

		val remainingDelta = newValue - originalValue
		return Offset.Zero.copy(y = remainingDelta)
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

	override fun restore(value: Float): ScrollConnectedScaler =
		ScrollConnectedScaler(max, min, value)

	override fun SaverScope.save(value: ScrollConnectedScaler): Float = value.totalDistanceTraveled
}
