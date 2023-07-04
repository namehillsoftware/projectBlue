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

	override fun SaverScope.save(value: ScrollConnectedScaler): Float = value.progress.blockingFirst()
}

class ScrollConnectedScaler(private val max: Float, private val min: Float, initialProgress: Float = 0f) : NestedScrollConnection {

	private val fullDistance = max - min

	private val valueState = BehaviorSubject.createDefault(max - (fullDistance * initialProgress))

	val progress = valueState
		.map { newValue -> (max - newValue) / fullDistance }
		.replay(1)
		.publish()
		.refCount()

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		val originalValue = valueState.value ?: 0f
		val newOffset = originalValue + delta
		val newValue = newOffset.coerceIn(min, max)

		if (newValue != valueState.value)
			valueState.onNext(newValue)

		Log.d(logTag, "newValue: $newValue")

		// here's the catch: let's pretend we consumed 0 in any case, since we want
		// LazyColumn to scroll anyway for good UX
		// We're basically watching scroll without taking it
		return Offset.Zero.copy(y = newValue - originalValue)
	}

	@Composable
	fun rememberProgress() = progress.subscribeAsState(initial = (max - (valueState.value ?: 0f)) / fullDistance)

	@Composable
	fun rememberValue() = valueState.subscribeAsState(initial = valueState.value ?: 0f)

	fun goToMax() {
		valueState.onNext(max)
	}

	fun goToMin() {
		valueState.onNext(min)
	}
}
