package com.lasthopesoftware.bluewater.shared.android.ui.components.dragging

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

class DragDropLazyColumnScope(private val inner: LazyListScope, private val dragDropListState: DragDropListState) : LazyListScope by inner {

	@OptIn(ExperimentalFoundationApi::class)
	fun <T> dragDropItems(items: List<T>, keyFactory: ((Int, T) -> Any), content: @Composable DragDropItemScope.(Int, T) -> Unit) {
		itemsIndexed(items, keyFactory) { index, item ->
			val dragModifier = with (this@DragDropLazyColumnScope.dragDropListState) {
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

            Box(modifier = dragModifier) {
                val scope = DragDropItemScope(
                    this@itemsIndexed,
                    keyFactory(index, item),
                    this@DragDropLazyColumnScope.dragDropListState
                )
                scope.content(index, item)
            }
		}
	}
}
