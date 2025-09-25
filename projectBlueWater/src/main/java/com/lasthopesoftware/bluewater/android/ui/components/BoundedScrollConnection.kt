package com.lasthopesoftware.bluewater.android.ui.components

import android.annotation.SuppressLint
import android.os.Parcelable
import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.lasthopesoftware.compilation.DebugFlag
import kotlinx.parcelize.Parcelize
import kotlin.math.abs

interface BoundedScrollConnection : NestedScrollConnection {
	fun goToMax()
	fun goToMin()
}

object NoOpBoundedScrollConnection : BoundedScrollConnection {
	override fun goToMax() {}

	override fun goToMin() {}
}

@Composable
fun rememberAnchoredScrollConnectionState(progressAnchors: FloatArray, progress: Float = 0f, selectedProgress: Float? = null): MutableAnchoredScrollConnectionState {
	return rememberSaveable(progressAnchors, saver = MutableAnchoredScrollConnectionState.Saver) {
		MutableAnchoredScrollConnectionState(progressAnchors, progress, selectedProgress)
	}
}

interface AnchoredScrollConnectionState {
	val progressAnchors: FloatArray
	val progress: Float
	val selectedProgress: Float?
}

class MutableAnchoredScrollConnectionState(
	override val progressAnchors: FloatArray,
	progress: Float,
	selectedProgress: Float? = null,
) : AnchoredScrollConnectionState {
	override var selectedProgress by mutableStateOf(selectedProgress)
	override var progress by mutableFloatStateOf(progress)

	object Saver : androidx.compose.runtime.saveable.Saver<MutableAnchoredScrollConnectionState, Triple<FloatArray, Float, Float?>> {
		override fun restore(value: Triple<FloatArray, Float, Float?>): MutableAnchoredScrollConnectionState =
			MutableAnchoredScrollConnectionState(value.first, value.second, value.third)

		override fun SaverScope.save(value: MutableAnchoredScrollConnectionState): Triple<FloatArray, Float, Float?> =
			Triple(value.progressAnchors, value.progress, value.selectedProgress)
	}
}

@SuppressLint("LongLogTag")
class AnchoredProgressScrollConnectionDispatcher(
	state: AnchoredScrollConnectionState,
	private val fullDistance: Float,
	private val inner: BoundedScrollConnection = NoOpBoundedScrollConnection,
) : BoundedScrollConnection by inner {

	companion object {
		private const val logTag = "AnchoredProgressScrollConnectionDispatcher"

		@Composable
		fun remember(state: AnchoredScrollConnectionState, inner: BoundedScrollConnection = NoOpBoundedScrollConnection, fullDistance: Float = Float.MAX_VALUE): AnchoredProgressScrollConnectionDispatcher {
			val dispatcher = remember(state, fullDistance, inner) {
				AnchoredProgressScrollConnectionDispatcher(
					state,
					fullDistance,
					inner
				)
			}

			LaunchedEffect(dispatcher) {
				if (state is MutableAnchoredScrollConnectionState) {
					snapshotFlow { dispatcher.selectedProgressState }
						.collect { state.selectedProgress = it }
				}
			}

			LaunchedEffect(dispatcher) {
				if (state is MutableAnchoredScrollConnectionState) {
					if (fullDistance != 0f) {
						snapshotFlow { dispatcher.totalDistanceTraveled }
							.collect {
								state.progress = (it / fullDistance).coerceIn(0f, 1f)
							}
					} else {
						state.progress = 0f
					}
				}
			}

			return dispatcher
		}
	}

	private val relativeAnchors = state.progressAnchors

	private var selectedProgressState by mutableStateOf(state.selectedProgress)

	private var totalDistanceTraveled by mutableFloatStateOf(state.progress * fullDistance)

	@SuppressLint("LongLogTag")
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		selectedProgressState = null
		totalDistanceTraveled += available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
		}

		return inner.onPreScroll(available, source)
	}

	@SuppressLint("LongLogTag")
	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		val innerConsumed = inner.onPostScroll(consumed, available, source)

		val available = available - innerConsumed

		totalDistanceTraveled -= available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return innerConsumed
	}

	override fun goToMin() {
		progressTo(relativeAnchors.first())
	}

	override fun goToMax() {
		progressTo(relativeAnchors.last())
	}

	fun progressTo(progress: Float) {
		val offset = progress * fullDistance
		val distanceToTravel = offset - totalDistanceTraveled
		val fakeAvailable = Offset(x= 0f, y = distanceToTravel)
		val consumed = onPreScroll(fakeAvailable, NestedScrollSource.UserInput)
		selectedProgressState = progress
		val adjustedConsumed = fakeAvailable - consumed
		onPostScroll(adjustedConsumed, fakeAvailable - adjustedConsumed, NestedScrollSource.UserInput)
	}
}

interface ScalerState {
	val min: Float
	val max: Float
	val totalDistanceTraveled: Float
}

@Parcelize
private data class MutableScalerState(
	override val min: Float,
	override val max: Float,
	override var totalDistanceTraveled: Float = 0f,
) : Parcelable, ScalerState

@Composable
fun rememberFullScreenScrollConnectedScalerState(min: Float, max: Float): ScalerState = rememberSaveable(max, min) {
	MutableScalerState(min, max)
}

class FullScreenScrollConnectedScaler(
	private val state: ScalerState,
	maxTravelDistance: Float = Float.MAX_VALUE,
) : BoundedScrollConnection {

	companion object {
		private const val logTag = "FullScreenScrollConnectedScaler"

		/**
		 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
		 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
		 */
		@Composable
		fun remember(min: Float, max: Float, maxTravelDistance: Float = Float.MAX_VALUE): FullScreenScrollConnectedScaler {
			val scalerState = rememberFullScreenScrollConnectedScalerState(min, max)

			return remember(scalerState, maxTravelDistance)
		}

		@Composable
		fun remember(scalerState: ScalerState, maxTravelDistance: Float = Float.MAX_VALUE): FullScreenScrollConnectedScaler {
			val scaler = remember(scalerState, maxTravelDistance) {
				FullScreenScrollConnectedScaler(scalerState, maxTravelDistance)
			}

			LaunchedEffect(scaler) {
				if (scalerState is MutableScalerState) {
					snapshotFlow { scaler.totalDistanceTraveled }
						.collect {
							scalerState.totalDistanceTraveled = it
						}
				}
			}

			return scaler
		}
	}

	private val absoluteMax = abs(maxTravelDistance)
	private val fullDistance = state.max - state.min

	private var totalDistanceTraveled by mutableFloatStateOf(keepWithinMaxTravelDistance(state.totalDistanceTraveled))

	val valueState by lazy {
		derivedStateOf {
			(state.max + totalDistanceTraveled).coerceIn(state.min, state.max)
		}
	}

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	@SuppressLint("LongLogTag")
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val originalValue = valueState.value
		totalDistanceTraveled = keepWithinMaxTravelDistance(totalDistanceTraveled + available.y)

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "valueState.value: ${valueState.value}")
		}

		return available.copy(y = valueState.value - originalValue)
	}

	@SuppressLint("LongLogTag")
	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		val originalValue = valueState.value
		totalDistanceTraveled = keepWithinMaxTravelDistance(totalDistanceTraveled - available.y)

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return available.copy(y = valueState.value - originalValue)
	}

	override fun goToMax() {
		totalDistanceTraveled = keepWithinMaxTravelDistance(0f)
	}

	override fun goToMin() {
		totalDistanceTraveled = keepWithinMaxTravelDistance(-fullDistance)
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else (state.max - value) / fullDistance

	private fun keepWithinMaxTravelDistance(value: Float): Float = value.coerceIn(-absoluteMax, absoluteMax)
}

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun rememberDeferredPreScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(saver = DeferredPreScrollConnectedScaler.Saver) {
	DeferredPreScrollConnectedScaler(max, min)
}

class DeferredPreScrollConnectedScaler private constructor(private val max: Float, private val min: Float, initialDistanceTraveled: Float) : BoundedScrollConnection {

	companion object {
		private const val logTag = "DeferredPreScrollConnectedScaler"
	}

	constructor(max: Float, min: Float): this(max, min, max)

	private val fullDistance = max - min

	private var preScrollAvailableOffset = Offset.Zero

	private val mutableValueState = mutableFloatStateOf(initialDistanceTraveled)

	val valueState = mutableValueState.asFloatState()

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// Defer consumption of pre-scroll until it is known how much was used by the lazylist, as we want to use
		// the same as the lazylist used.
		preScrollAvailableOffset = available
		return Offset.Zero
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		consumeY(preScrollAvailableOffset - available)

		// Do not report any offset consumed, as it will be using pre-scroll available offset.
		return Offset.Zero
	}

	@SuppressLint("LongLogTag")
	private fun consumeY(available: Offset): Offset {
		val delta = available.y
		val originalValue = mutableValueState.floatValue
		val newOffset = originalValue + delta
		mutableValueState.floatValue = newOffset.coerceIn(min, max)

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "valueState.value: ${valueState.floatValue}")
		}

		return available.copy(y = mutableValueState.floatValue - originalValue)
	}

	override fun goToMax() {
		mutableValueState.floatValue = max
	}

	suspend fun animateGoToMax(animationSpec: AnimationSpec<Float> = tween()) {
		animate(
			initialValue = mutableValueState.floatValue,
			targetValue = max,
			animationSpec = animationSpec,
		) { value, _ ->
			mutableValueState.floatValue = value
		}
	}

	override fun goToMin() {
		mutableValueState.floatValue = min
	}

	suspend fun animateGoToMin(animationSpec: AnimationSpec<Float> = tween()) {
		animate(
			initialValue = mutableValueState.floatValue,
			targetValue = min,
			animationSpec = animationSpec,
		) { value, _ ->
			mutableValueState.floatValue = value
		}
	}

	private fun calculateProgress(value: Float) = if (fullDistance == 0f) 1f else (max - value) / fullDistance

	object Saver : androidx.compose.runtime.saveable.Saver<DeferredPreScrollConnectedScaler, Triple<Float, Float, Float>> {
		override fun restore(value: Triple<Float, Float, Float>): DeferredPreScrollConnectedScaler =
			DeferredPreScrollConnectedScaler(value.first, value.second, value.third)

		override fun SaverScope.save(value: DeferredPreScrollConnectedScaler): Triple<Float, Float, Float> =
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

	// Propagate post- events in reverse order
	override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
		val nextConsumed = second.onPostFling(consumed, available)
		return first.onPostFling(consumed + nextConsumed, available - nextConsumed)
	}

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val consumed = first.onPreScroll(available, source)
		return second.onPreScroll(available - consumed, source)
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		val nextConsumed = second.onPostScroll(consumed, available, source)
		return first.onPostScroll(consumed + nextConsumed, available - nextConsumed, source)
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

fun BoundedScrollConnection.linkedTo(other: BoundedScrollConnection) =
	LinkedNestedScrollConnection(this, other)

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

fun BoundedScrollConnection.ignoreConsumedOffset() =
	ConsumedOffsetErasingNestedScrollConnection(this)
