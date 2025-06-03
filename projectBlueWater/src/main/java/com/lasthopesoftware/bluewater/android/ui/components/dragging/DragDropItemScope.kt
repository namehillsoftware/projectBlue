package com.lasthopesoftware.bluewater.android.ui.components.dragging

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class DragDropItemScope(private val inner: LazyItemScope, private val key: Any, private val state: DragDropListState) : LazyItemScope by inner {

	fun Modifier.detectDrag() = pointerInput(Unit) {
		detectDragGestures(
			onDragStart = {
				state.onDragStart(key)
				state.onDrag(it)
			},
			onDrag = { change, offset ->
				change.consume()
				state.onDrag(offset)
			},
			onDragEnd = { state.onDragEnd() },
			onDragCancel = { state.onDragInterrupted() }
		)
	}
}
