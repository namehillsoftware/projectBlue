package com.lasthopesoftware.bluewater.android.ui.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.resources.closables.AutoCloseableManager

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
	private val state: MutableAnchoredScrollConnectionState,
	private val fullDistance: Float,
	private val inner: BoundedScrollConnection = NoOpBoundedScrollConnection,
) : BoundedScrollConnection by inner, AutoCloseable {

	companion object {
		private const val logTag = "AnchoredProgressScrollConnectionDispatcher"
	}

	private val autoCloseableManager = AutoCloseableManager()

	private val selectedProgressState = MutableInteractionState(state.selectedProgress)

	private val relativeAnchors = state.progressAnchors

	private val totalDistanceTraveled = MutableInteractionState(state.progress * fullDistance)

	init {
		autoCloseableManager.manage(
			selectedProgressState
				.subscribe {
					if (DebugFlag.isDebugCompilation) {
						Log.d(logTag, "selectedProgress: $it")
					}
					state.selectedProgress = it.value
				}
				.toCloseable()
		)

		if (fullDistance != 0f) {
			autoCloseableManager.manage(
				totalDistanceTraveled
					.mapNotNull()
					.subscribe {
						state.progress = (it / fullDistance).coerceIn(0f, 1f)
						if (DebugFlag.isDebugCompilation) {
							Log.d(logTag, "state.progress: ${state.progress}")
						}
					}
					.toCloseable()
			)
		} else {
			state.progress = 0f
		}
	}

	override fun close() {
		autoCloseableManager.close()
	}

	@SuppressLint("LongLogTag")
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		selectedProgressState.value = null
		totalDistanceTraveled.value += available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${totalDistanceTraveled.value}")
		}

		return inner.onPreScroll(available, source)
	}

	@SuppressLint("LongLogTag")
	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		val innerConsumed = inner.onPostScroll(consumed, available, source)

		val available = available - innerConsumed

		totalDistanceTraveled.value -= available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: ${totalDistanceTraveled.value}")
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
		val distanceToTravel = offset - totalDistanceTraveled.value
		onPreScroll(Offset(x= 0f, y = distanceToTravel), NestedScrollSource.UserInput)
		selectedProgressState.value = progress
	}
}

/**
 * Will scale a value based on a nested **vertical** scroll, sourced from Android examples on this page:
 * https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#extension-functions
 */
@Composable
fun rememberFullScreenScrollConnectedScaler(max: Float, min: Float) = rememberSaveable(max, min, saver = FullScreenScrollConnectedScaler.Saver) {
	FullScreenScrollConnectedScaler(max, min)
}

class FullScreenScrollConnectedScaler private constructor(
	private val max: Float,
	private val min: Float,
	initialDistanceTraveled: Float
) : BoundedScrollConnection {

	companion object {
		private const val logTag = "FullScreenScrollConnectedScaler"
	}

	constructor(max: Float, min: Float): this(max, min, 0f)

	private val fullDistance = max - min

	private var totalDistanceTraveled by mutableFloatStateOf(initialDistanceTraveled)

	val valueState = derivedStateOf { (max + totalDistanceTraveled).coerceIn(min, max) }

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	@SuppressLint("LongLogTag")
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val originalValue = valueState.value
		totalDistanceTraveled += available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "valueState.value: ${valueState.value}")
		}

		return available.copy(y = valueState.value - originalValue)
	}

	@SuppressLint("LongLogTag")
	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		val originalValue = valueState.value
		totalDistanceTraveled -= available.y

		if (DebugFlag.isDebugCompilation) {
			Log.d(logTag, "totalDistanceTraveled: $totalDistanceTraveled")
			Log.d(logTag, "consumed: ${consumed.y}")
			Log.d(logTag, "available: ${available.y}")
		}

		return available.copy(y = valueState.value - originalValue)
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

	companion object {
		private const val logTag = "PreScrollConnectedScaler"
	}

	constructor(max: Float, min: Float): this(max, min, max)

	private val fullDistance = max - min

	private var totalDistanceTraveled by mutableFloatStateOf(initialDistanceTraveled)

	private val mutableValueState = mutableFloatStateOf(initialDistanceTraveled)

	val valueState = mutableValueState.asFloatState()

	val progressState by lazy { derivedStateOf { calculateProgress(valueState.value) } }

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		return consumeY(available)
	}

	override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
		consumeY(-available)
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
