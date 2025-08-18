package com.lasthopesoftware.bluewater.android.ui.components

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastRoundToInt
import com.lasthopesoftware.bluewater.android.ui.linearInterpolation
import com.lasthopesoftware.compilation.DebugFlag

private const val logTag = "ScrollConnectedScaler"

interface BoundedScrollConnection : NestedScrollConnection {
	fun goToMax()
	fun goToMin()
}

@Composable
fun <T : Parcelable> rememberAnchoredScrollConnectionDispatcher(
	lazyListState: LazyListState,
	anchors: Map<T, Float>,
	inner: BoundedScrollConnection,
): AnchoredScrollConnectionDispatcher<T> {
	val state = rememberSaveable(anchors, saver = AnchoredScrollConnectionDispatcher.AnchoredScrollConnectionState.Saver()) {
		AnchoredScrollConnectionDispatcher.AnchoredScrollConnectionState(anchors, 0f)
	}

	return remember(inner, lazyListState, state) {
		AnchoredScrollConnectionDispatcher(lazyListState, state, inner)
	}
}

class AnchoredScrollConnectionDispatcher<T : Parcelable>(
	private val lazyListState: LazyListState,
	private val state: AnchoredScrollConnectionState<T>,
	private val inner: BoundedScrollConnection,
) : BoundedScrollConnection by inner {
	private val min by lazy { state.anchors.values.first() }
	private val max by lazy { state.anchors.values.last() }
	private val fullDistance by lazy { max - min }

	val valueState by derivedStateOf { (max + state.totalDistanceTraveled).coerceIn(min, max) }

	val progressState by lazy { derivedStateOf { calculateProgress(valueState) } }

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		state.totalDistanceTraveled += available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${state.totalDistanceTraveled}")
		}

		return inner.onPreScroll(available, source)
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		state.totalDistanceTraveled -= available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${state.totalDistanceTraveled}")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return inner.onPostScroll(consumed, available, source)
	}

	override fun goToMin() {
		goToOffset(min)
	}

	override fun goToMax() {
		goToOffset(max)
	}

	fun goTo(anchor: T) {
		state.anchors[anchor]?.also(::goToOffset)
	}

	private fun goToOffset(offset: Float) {
		val distanceToTravel = offset - state.totalDistanceTraveled
		onPreScroll(Offset(x= 0f, y = distanceToTravel), NestedScrollSource.UserInput)
		val selectedIndex = linearInterpolation(0f, (lazyListState.layoutInfo.totalItemsCount - 1).toFloat(), calculateProgress(offset)).fastRoundToInt()
		lazyListState.requestScrollToItem(selectedIndex)
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else value / fullDistance

	class AnchoredScrollConnectionState<T : Parcelable>(
		val anchors: Map<T, Float>,
		initialDistanceTraveled: Float,
	) {
		var totalDistanceTraveled by mutableFloatStateOf(initialDistanceTraveled)

		class Saver<T : Parcelable> : androidx.compose.runtime.saveable.Saver<AnchoredScrollConnectionState<T>, Pair<Map<T, Float>, Float>> {
			override fun restore(value: Pair<Map<T, Float>, Float>): AnchoredScrollConnectionState<T> =
				AnchoredScrollConnectionState(anchors = value.first, value.second)

			override fun SaverScope.save(value: AnchoredScrollConnectionState<T>): Pair<Map<T, Float>, Float> =
				Pair(value.anchors,value.totalDistanceTraveled)
		}
	}
}

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun rememberFullScreenScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = FullScreenScrollConnectedScaler.Saver) {
	FullScreenScrollConnectedScaler(max, min)
}

class FullScreenScrollConnectedScaler private constructor(
	private val max: Float,
	private val min: Float,
	initialDistanceTraveled: Float
) : BoundedScrollConnection {

	constructor(max: Float, min: Float): this(max, min, 0f)

	private val fullDistance = max - min

	private var totalDistanceTraveled by mutableFloatStateOf(initialDistanceTraveled)

	val valueState = derivedStateOf { (max + totalDistanceTraveled).coerceIn(min, max) }

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		val originalValue = valueState.value
		totalDistanceTraveled += delta

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "valueState.value: ${valueState.value}")
		}

		return available.copy(y = valueState.value - originalValue)
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		totalDistanceTraveled -= available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return super.onPostScroll(consumed, available, source)
	}

	override fun goToMax() {
		totalDistanceTraveled = 0f
	}

	override fun goToMin() {
		totalDistanceTraveled = -fullDistance
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else (max - value) / fullDistance

	object Saver : androidx.compose.runtime.saveable.Saver<FullScreenScrollConnectedScaler, Triple<Float, Float, Float>> {
		override fun restore(value: Triple<Float, Float, Float>): FullScreenScrollConnectedScaler =
			FullScreenScrollConnectedScaler(value.first, value.second, value.third)

		override fun SaverScope.save(value: FullScreenScrollConnectedScaler): Triple<Float, Float, Float> =
			Triple(value.max, value.min, value.totalDistanceTraveled)
	}
}

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun rememberPreScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = PreScrollConnectedScaler.Saver) {
	PreScrollConnectedScaler(max, min)
}

class PreScrollConnectedScaler private constructor(private val max: Float, private val min: Float, initialDistanceTraveled: Float) : BoundedScrollConnection {

	constructor(max: Float, min: Float): this(max, min, max)

	private val fullDistance = max - min

	private val mutableValueState = mutableFloatStateOf(initialDistanceTraveled)

	val valueState = mutableValueState.asFloatState()

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		return consumeY(available)
	}

	private fun consumeY(available: Offset): Offset {
		val delta = available.y
		val originalValue = mutableValueState.floatValue
		val newOffset = originalValue + delta
		mutableValueState.floatValue = newOffset.coerceIn(min, max)

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "valueState.value: ${valueState.value}")
		}

		return available.copy(y = mutableValueState.floatValue - originalValue)
	}

	override fun goToMax() {
		mutableValueState.floatValue = max
	}

	override fun goToMin() {
		mutableValueState.floatValue = min
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else (max - value) / fullDistance

	object Saver : androidx.compose.runtime.saveable.Saver<PreScrollConnectedScaler, Triple<Float, Float, Float>> {
		override fun restore(value: Triple<Float, Float, Float>): PreScrollConnectedScaler =
			PreScrollConnectedScaler(value.first, value.second, value.third)

		override fun SaverScope.save(value: PreScrollConnectedScaler): Triple<Float, Float, Float> =
			Triple(value.max, value.min, value.valueState.floatValue)
	}
}

class LinkedNestedScrollConnection(
	private val first: BoundedScrollConnection,
	private val second: BoundedScrollConnection
) : BoundedScrollConnection {
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

	override fun goToMax() {
		first.goToMax()
		second.goToMax()
	}

	override fun goToMin() {
		first.goToMin()
		second.goToMin()
	}
}

class ConsumedOffsetErasingNestedScrollConnection(private val inner: BoundedScrollConnection) : BoundedScrollConnection by inner {
	override suspend fun onPreFling(available: Velocity): Velocity {
		inner.onPreFling(available)
		return Velocity.Zero
	}

	override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
		inner.onPostFling(consumed, available)
		return Velocity.Zero
	}

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		inner.onPreScroll(available, source)
		return Offset.Zero
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		inner.onPostScroll(consumed, available, source)
		return Offset.Zero
	}
}
