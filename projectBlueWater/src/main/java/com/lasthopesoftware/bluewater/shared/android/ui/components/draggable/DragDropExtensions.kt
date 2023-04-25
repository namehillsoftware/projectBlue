package com.lasthopesoftware.bluewater.shared.android.ui.components.draggable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.signalDrag(dragDropListState: DragDropListState): Modifier {
	return pointerInput(Unit) {
		awaitEachGesture {
			awaitFirstDown(requireUnconsumed = false)
			dragDropListState.isDragEnabled = true
			waitForUpOrCancellation()
			dragDropListState.isDragEnabled = false
		}
	}
}

fun Modifier.detectDrag(dragDropListState: DragDropListState) =
	pointerInput(Unit) {
		detectDragGestures(
			onDrag = { change, offset ->
				if (dragDropListState.isDragEnabled) {
					change.consume()
					dragDropListState.onDrag(offset)
				}
			},
			onDragStart = { offset ->
				if (dragDropListState.isDragEnabled)
					dragDropListState.onDragStart(offset)
		  	},
			onDragEnd = { dragDropListState.onDragEnd() },
			onDragCancel = { dragDropListState.onDragInterrupted() }
		)
	}

/*
    LazyListItemInfo.index is the item's absolute index in the list
    Based on the item's "relative position" with the "currently top" visible item,
    this returns LazyListItemInfo corresponding to it
*/
fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? {
	return this.layoutInfo.visibleItemsInfo.getOrNull(absoluteIndex - this.layoutInfo.visibleItemsInfo.first().index)
}

/*
  Bottom offset of the element in Vertical list
*/
val LazyListItemInfo.offsetEnd: Int
	get() = this.offset + this.size

/*
   Moving element in the list
*/
fun <T> MutableList<T>.move(from: Int, to: Int) {
	if (from == to)
		return

	val element = this.removeAt(from) ?: return
	this.add(to, element)
}
