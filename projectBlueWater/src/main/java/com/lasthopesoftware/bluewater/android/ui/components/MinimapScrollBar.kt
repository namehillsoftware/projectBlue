package com.lasthopesoftware.bluewater.android.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor
import kotlin.math.round

private val anchorSize = viewPaddingUnit * 2
private val anchorVerticalPadding = viewPaddingUnit * 3
private val totalAnchorSize = anchorSize + anchorVerticalPadding * 2

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MinimapScrollBar(modifier: Modifier = Modifier, onScrollProgress: (Float) -> Unit) {
	BoxWithConstraints(modifier = modifier) {
		val maxAnchors = (maxHeight.value / totalAnchorSize.value).toInt()
		val anchors = 0 until maxAnchors
		val anchorPercentages = anchors.map { it / maxAnchors.toFloat() }
		var draggedPosition = remember { 0f }
		Column(
			modifier = Modifier
				.fillMaxHeight()
				.pointerInput(Unit) {
					detectVerticalDragGestures(
						onDragStart = { startingOffset ->
							draggedPosition = startingOffset.y
						}
					) { _, dragAmount ->
						draggedPosition += dragAmount
						val progress = (draggedPosition / maxHeight.toPx()).coerceIn(0f, 1f)
						val selectedAnchor = anchorPercentages.firstOrNull { it == progress }
						if (selectedAnchor != null)
							onScrollProgress(selectedAnchor)
						else
							onScrollProgress(round(progress * 100) / 100)
					}
				}
				.align(Alignment.CenterEnd),
		) {
			for (i in anchors) {
				Box(
					modifier = Modifier
						.onGloballyPositioned({ coords -> coords.positionInParent() })
						.padding(horizontal = viewPaddingUnit * 2, vertical = anchorVerticalPadding)
						.background(color = LocalControlColor.current, shape = CircleShape)
						.size(viewPaddingUnit * 2)
						.navigable(
							onClick = { onScrollProgress((i / maxAnchors.toFloat()).coerceIn(0f, 1f)) }
						)
				)
			}
		}
	}
}
