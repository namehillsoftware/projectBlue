package com.lasthopesoftware.bluewater.shared.android.ui.components.draggable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
inline fun <T> DragDropLazyColumn(
	items: List<T>,
	noinline key: ((Int, T) -> Any),
	dragDropListState: DragDropListState,
	modifier: Modifier = Modifier,
	crossinline item: @Composable LazyItemScope.(T, Int) -> Unit
) {
	LazyColumn(
		modifier = modifier.detectDrag(dragDropListState),
		state = dragDropListState.lazyListState
	) {
		itemsIndexed(items, key) { index, item ->
			val current by animateFloatAsState(targetValue = (dragDropListState.elementDisplacement ?: 0f) * 0.67f)

			val itemModifier = dragDropListState.elementDisplacement
				.takeIf {
					index == dragDropListState.currentIndexOfDraggedItem
				}
				?.let {
					Modifier
						.graphicsLayer {
							translationY = it
						}
				}
				?: Modifier

			Box(
				modifier = itemModifier
			) { item(item, index) }
		}
	}
}
