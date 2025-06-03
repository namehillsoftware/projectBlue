package com.lasthopesoftware.bluewater.android.ui.components.dragging

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState

/*
    LazyListItemInfo.index is the item's absolute index in the list
    Based on the item's "relative position" with the "currently top" visible item,
    this returns LazyListItemInfo corresponding to it
*/
fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? {
	return this.layoutInfo.visibleItemsInfo.getVisibleItemInfoFor(absoluteIndex)
}

fun List<LazyListItemInfo>.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? {
	return this.getOrNull(absoluteIndex - (firstOrNull()?.index ?: 0))
}

/*
  Bottom offset of the element in Vertical list
*/
val LazyListItemInfo.offsetEnd: Int
	get() = this.offset + this.size

val LazyListState.firstVisibleItem: LazyListItemInfo?
	get() = layoutInfo.visibleItemsInfo.firstOrNull()

val LazyListState.lastVisibleItem: LazyListItemInfo?
	get() = layoutInfo.visibleItemsInfo.lastOrNull()

/*
	Returns true if the
 */
val LazyListState.isAtLocalEnd: Boolean
	get() = lastVisibleItem?.run { layoutInfo.viewportEndOffset - offsetEnd } == 0

/*
   Moving element in the list
*/
fun <T> MutableList<T>.move(from: Int, to: Int): MutableList<T> {
	if (from == to) return this

	val element = this.removeAt(from) ?: return this
	this.add(to, element)

	return this
}
