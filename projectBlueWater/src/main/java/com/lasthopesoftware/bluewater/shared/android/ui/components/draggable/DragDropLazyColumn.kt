package com.lasthopesoftware.bluewater.shared.android.ui.components.draggable

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
inline fun DragDropLazyColumn(
	dragDropListState: DragDropListState,
	modifier: Modifier = Modifier,
	crossinline content: DragDropLazyColumnScope.() -> Unit
) {
	LazyColumn(
		modifier = modifier,
		state = dragDropListState.lazyListState
	) {
		DragDropLazyColumnScope(this, dragDropListState).content()
	}
}

