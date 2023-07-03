package com.lasthopesoftware.bluewater.shared.android.ui.components

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

private const val logTag = "CollapsibleHeight"

class CollapsibleSaver(private val expandedHeight: Float, private val collapsedHeight: Float) : Saver<CollapsibleHeight, Float> {
	override fun restore(value: Float): CollapsibleHeight =
		CollapsibleHeight(expandedHeight, collapsedHeight, value)

	override fun SaverScope.save(value: CollapsibleHeight): Float = value.progress

}

class CollapsibleHeight(private val expandedHeight: Float, private val collapsedHeight: Float, initialProgress: Float = 0f) : NestedScrollConnection {

	private val fullDistance = expandedHeight - collapsedHeight

	var height by mutableStateOf(expandedHeight - (fullDistance * initialProgress))
		private set

	var progress by mutableStateOf(initialProgress)
		private set

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		// try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
		val delta = available.y
		val newOffset = height + delta
		height = newOffset.coerceIn(collapsedHeight, expandedHeight)
		progress = (expandedHeight - height) / fullDistance

		Log.d(logTag, "height: $height, progress: $progress")

		// here's the catch: let's pretend we consumed 0 in any case, since we want
		// LazyColumn to scroll anyway for good UX
		// We're basically watching scroll without taking it
		return Offset.Zero
	}
}
