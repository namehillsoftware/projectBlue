package com.lasthopesoftware.bluewater.shared.android.ui.components.draggable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

@OptIn(ExperimentalFoundationApi::class)
@Composable
inline fun <T> DragDropLazyColumn(
	items: List<T>,
	noinline keyFactory: ((Int, T) -> Any),
	dragDropListState: DragDropListState,
	modifier: Modifier = Modifier,
	crossinline item: @Composable DragDropItemScope.(Int, T) -> Unit
) {
	LazyColumn(
		modifier = modifier,
		state = dragDropListState.lazyListState
	) {
		itemsIndexed(items, keyFactory) { index, item ->
			val dragModifier = with (dragDropListState) {
				@Suppress("IntroduceWhenSubject")
				when {
					index == currentIndexOfDraggedItem -> Modifier
						.zIndex(1f)
						.graphicsLayer {
							translationY = draggingItemOffset
						}

					index == previousIndexOfDraggedItem -> Modifier
						.zIndex(1f)
						.graphicsLayer {
							translationY = previousItemOffset.value
						}

					else -> Modifier.animateItemPlacement()
				}
			}

			Column(modifier = dragModifier) {
				val scope = DragDropItemScope(this@itemsIndexed, keyFactory(index, item), dragDropListState)
				scope.item(index, item)
			}
		}
	}
}
