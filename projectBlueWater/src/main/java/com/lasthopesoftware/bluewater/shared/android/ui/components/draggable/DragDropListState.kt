package com.lasthopesoftware.bluewater.shared.android.ui.components.draggable

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun rememberDragDropListState(
	lazyListState: LazyListState = rememberLazyListState(),
	onMove: (Int, Int) -> Unit,
	onDragEnd: ((Int, Int) -> Unit)? = null
): DragDropListState {
	val scope = rememberCoroutineScope()
	return remember(lazyListState) { DragDropListState(lazyListState = lazyListState, scope, onMove = onMove, onDragEnd) }
}

class DragDropListState(
	val lazyListState: LazyListState,
	private val scope: CoroutineScope,
	private val onMove: (Int, Int) -> Unit,
	private val onDragEnd: ((Int, Int) -> Unit)? = null
) {
	var draggedDistance by mutableStateOf(0f)

	// used to obtain initial offsets on drag start
	var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

	var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

	var isDragEnabled by mutableStateOf(false)

	val initialOffsets: Pair<Int, Int>?
		get() = initiallyDraggedElement?.let { Pair(it.offset, it.offsetEnd) }

	val elementDisplacement: Float?
		get() = currentElement
			?.let { item -> (initiallyDraggedElement?.offset ?: 0f).toFloat() + draggedDistance - item.offset }

	internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
		private set

	val currentElement: LazyListItemInfo?
		get() = currentIndexOfDraggedItem?.let {
			lazyListState.getVisibleItemInfoFor(absoluteIndex = it)
		}

	var overscrollJob by mutableStateOf<Job?>(null)

	fun onDragStart(offset: Offset) {
		val draggedItem = lazyListState.layoutInfo
			.visibleItemsInfo
			.firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
			?: return

		currentIndexOfDraggedItem = draggedItem.index
		initiallyDraggedElement = draggedItem
	}

	fun onDragEnd() {
		initiallyDraggedElement?.index?.also { initialPosition ->
			currentIndexOfDraggedItem?.also { finalPosition ->
				onDragEnd?.invoke(initialPosition, finalPosition)
			}
		}
		onDragInterrupted()
	}

	fun onDragInterrupted() {
		currentIndexOfDraggedItem?.also { current ->
			previousIndexOfDraggedItem = current
		}
		draggedDistance = 0f
		currentIndexOfDraggedItem = null
		initiallyDraggedElement = null
		overscrollJob?.cancel()
	}

	fun onDrag(offset: Offset) {
		draggedDistance += offset.y

		initialOffsets?.also { (topOffset, bottomOffset) ->
			val startOffset = topOffset + draggedDistance
			val endOffset = bottomOffset + draggedDistance

			currentElement?.also { hovered ->
				lazyListState.layoutInfo.visibleItemsInfo
					.filterNot { item -> item.offsetEnd < startOffset || item.offset > endOffset || hovered.index == item.index }
					.firstOrNull { item ->
						val delta = startOffset - hovered.offset
						when {
							delta > 0 -> (endOffset > item.offsetEnd)
							else -> (startOffset < item.offset)
						}
					}
					?.also { item ->
						currentIndexOfDraggedItem?.also { current -> onMove.invoke(current, item.index) }
						currentIndexOfDraggedItem = item.index
					}
			}
		}

		if (overscrollJob?.isActive == true) return

		val overScroll = checkForOverScroll()
		if (overScroll == 0f) {
			overscrollJob?.cancel()
			return
		}

		overscrollJob = scope.launch { lazyListState.animateScrollBy(overScroll) }
	}

	private fun checkForOverScroll(): Float {
		return initiallyDraggedElement?.let {
			val startOffset = it.offset + draggedDistance
			val endOffset = it.offsetEnd + draggedDistance

			with (lazyListState.layoutInfo) {
				when {
					draggedDistance > 0 -> (endOffset - viewportEndOffset).takeIf { diff -> diff > 0 }
					draggedDistance < 0 -> (startOffset - viewportStartOffset).takeIf { diff -> diff < 0 }
					else -> null
				}
			}
		} ?: 0f
	}
}
