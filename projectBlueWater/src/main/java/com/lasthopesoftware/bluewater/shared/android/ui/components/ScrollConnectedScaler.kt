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
fun memorableScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = ScrollConnectedScalerSaver(max, min)) {
	ScrollConnectedScaler(max, min)
}

class ScrollConnectedScalerSaver(private val max: Float, private val min: Float) : Saver<ScrollConnectedScaler, Float> {
	override fun restore(value: Float): ScrollConnectedScaler =
		ScrollConnectedScaler(max, min, value)

	override fun SaverScope.save(value: ScrollConnectedScaler): Float = value.currentProgress()
}

class ScrollConnectedScaler(private val max: Float, private val min: Float, initialProgress: Float = 0f) : NestedScrollConnection {

	private val fullDistance = max - min

	private val initialValue = max - (fullDistance * initialProgress)

	private val valueState = BehaviorSubject.createDefault(initialValue)

	private val progress = valueState
		.map(::calculateProgress)
		.replay(1)
		.refCount()

	private var totalDistanceTraveled = 0f

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		totalDistanceTraveled += delta
		val newValue = totalDistanceTraveled.coerceIn(min, max)

		val originalValue = currentValue()
		if (newValue != originalValue)
			valueState.onNext(newValue)

		Log.d(logTag, "newValue: $newValue")

		val remainingDelta = newValue - originalValue
		return Offset.Zero.copy(y = remainingDelta)
	}

	fun currentProgress(): Float = calculateProgress(currentValue())

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
}
