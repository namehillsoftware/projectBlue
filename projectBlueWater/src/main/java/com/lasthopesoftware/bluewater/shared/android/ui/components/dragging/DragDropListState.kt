package com.lasthopesoftware.bluewater.shared.android.ui.components.dragging

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun rememberDragDropListState(
	lazyListState: LazyListState = rememberLazyListState(),
	onMove: (Int, Int) -> Unit,
	onDragEnd: ((Int, Int) -> Unit)? = null
): DragDropListState {
	val scope = rememberCoroutineScope()
	return remember(lazyListState) {
		DragDropListState(lazyListState = lazyListState, scope, onMove = onMove, onDragEnd)
	}
}

class DragDropListState(
	val lazyListState: LazyListState,
	private val scope: CoroutineScope,
	private val onMove: (Int, Int) -> Unit,
	private val onDragEnd: ((Int, Int) -> Unit)? = null
) {
	companion object {
		private const val logTag = "DragDropListState"
	}

	private var scrollToNextIndexJob: Job? = null
	private var overscrollJob: Job? = null

	private var draggedDistance by mutableFloatStateOf(0f)

	// used to obtain initial offsets on drag start
	private var initiallyDraggedElementIndex: Int? = null
	private var initiallyDraggedElementOffset by mutableIntStateOf(0)

	var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
		private set

	val draggingItemOffset: Float
		get() = currentElement
			?.let { item -> initiallyDraggedElementOffset + draggedDistance - item.offset }
			?: 0f

	var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
		private set
	var previousItemOffset = Animatable(0f)
		private set

	private val currentElement: LazyListItemInfo?
		get() = currentIndexOfDraggedItem?.let {
			lazyListState.getVisibleItemInfoFor(absoluteIndex = it)
		}

	fun onDragStart(key: Any) {
		val draggedItem = lazyListState.layoutInfo
			.visibleItemsInfo
			.firstOrNull { item -> item.key == key }
			?: return

		Log.d(logTag, "draggedItem.index=${draggedItem.index}")

		draggedDistance = 0f
		currentIndexOfDraggedItem = draggedItem.index
		initiallyDraggedElementIndex = draggedItem.index
		initiallyDraggedElementOffset = draggedItem.offset
	}

	fun onDragEnd() {
		val initialPosition = initiallyDraggedElementIndex
		val finalPosition = currentIndexOfDraggedItem

		onDragInterrupted()

		initialPosition ?: return
		finalPosition ?: return

		onDragEnd?.invoke(initialPosition, finalPosition)
	}

	fun onDragInterrupted() {
		Log.d(logTag, "onDragInterrupted")
		currentIndexOfDraggedItem?.also { current ->
			scope.launch {
				try {
					overscrollJob?.join()
					scrollToNextIndexJob?.join()
				} catch (e: Throwable) {
					// ignored
				}

				previousIndexOfDraggedItem = current
				val startOffset = draggingItemOffset

				previousItemOffset.snapTo(startOffset)
				previousItemOffset.animateTo(
					0f,
					spring(
						stiffness = Spring.StiffnessMediumLow,
						visibilityThreshold = 1f
					)
				)
				previousIndexOfDraggedItem = null
			}
		}
		draggedDistance = 0f
		currentIndexOfDraggedItem = null
		initiallyDraggedElementIndex = null
		initiallyDraggedElementOffset = 0
		overscrollJob?.cancel()
	}

	fun onDrag(offset: Offset) {
		Log.d(logTag, "onDrag")

		val newDistanceTraveled = offset.y
		draggedDistance += newDistanceTraveled

		val draggingElement = currentElement ?: return
		val startOffset = draggingElement.offset + draggingItemOffset
		val middleOffset = draggingElement.size / 2 + startOffset
		val middleOffsetInt = when {
			newDistanceTraveled > 0 -> ceil(middleOffset).toInt()
			newDistanceTraveled < 0 -> floor(middleOffset).toInt()
			else -> middleOffset.roundToInt()
		}

		val targetItem = lazyListState.layoutInfo.visibleItemsInfo
			.firstOrNull { item ->
				draggingElement.index != item.index && middleOffsetInt in item.offset..item.offsetEnd
			}

		if (targetItem != null) {
			val scrollToNextItem = when (lazyListState.firstVisibleItemIndex) {
				targetItem.index -> draggingElement
				draggingElement.index -> targetItem
				else -> null
			}

			Log.d(logTag, "scrollToNextIndex=$scrollToNextItem")

			if (scrollToNextItem != null) {
				scrollToNextIndexJob?.cancel()
				scrollToNextIndexJob = scope.launch {
					Log.d(logTag, "scrollToNextIndexJob begin")
					// this is needed to neutralize automatic keeping the first item first.
					lazyListState.scrollBy(scrollToNextItem.offset.toFloat())
					onMove(draggingElement.index, targetItem.index)
					Log.d(logTag, "scrollToNextIndexJob end")
				}
			} else {
				onMove(draggingElement.index, targetItem.index)
			}

			currentIndexOfDraggedItem = targetItem.index
			return
		}

		if (overscrollJob?.isActive == true) return

		val endOffset = startOffset + draggingElement.size
		val overScroll = with (lazyListState.layoutInfo) {
			when {
				draggedDistance > 0 -> (endOffset - viewportEndOffset).coerceAtLeast(0f)
				draggedDistance < 0 -> (startOffset - viewportStartOffset).coerceAtMost(0f)
				else -> 0f
			}
		}
		if (overScroll == 0f) {
			overscrollJob?.cancel()
			return
		}

		overscrollJob = scope.launch { lazyListState.scrollBy(overScroll) }
	}
}
