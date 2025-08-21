package com.lasthopesoftware.bluewater.android.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor
import kotlin.math.abs
import kotlin.math.round

private val anchorSize = viewPaddingUnit * 2
private val minAnchorVerticalPadding = viewPaddingUnit
private val totalAnchorSize = anchorSize + minAnchorVerticalPadding * 2

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MinimapScrollBar(
	modifier: Modifier = Modifier,
	knownPoints: Set<Float> = emptySet(),
	onScrollProgress: ((Float) -> Unit)? = null,
	onSelected: ((Int) -> Unit)? = null
) {
	BoxWithConstraints(modifier = modifier) {
		val anchoredPercentages = remember( maxHeight) {
			val maxAnchors = (maxHeight.value / totalAnchorSize.value).toInt().coerceAtMost(19)
			val anchors = 0 .. maxAnchors
			val anchorCount = anchors.last().toFloat()
			val fractionStep = 1 / anchorCount
			val halfStep = fractionStep * .5f
			anchors.map { a ->
				val percentage = a / anchorCount
				val closestKnownPoint = knownPoints
					.filter { abs(percentage - it) < halfStep }
					.minByOrNull { abs(percentage - it) }
				Pair(a, closestKnownPoint ?: percentage)
			}
		}
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
						val selectedAnchor = anchoredPercentages.firstOrNull { (_, p) -> p == progress }
						if (selectedAnchor == null) {
							onScrollProgress?.invoke(round(progress * 100) / 100)
						} else {
							val (anchor, progress) = selectedAnchor
							onScrollProgress?.invoke(progress)
							onSelected?.invoke(anchor)
						}
					}
				},
			verticalArrangement = Arrangement.SpaceEvenly,
		) {
			for ((i, p) in anchoredPercentages) {
				Box(
					modifier = Modifier
						.padding(horizontal = viewPaddingUnit * 2, vertical = minAnchorVerticalPadding)
						.background(color = LocalControlColor.current, shape = CircleShape)
						.size(viewPaddingUnit * 2)
						.navigable(
							onClick = {
								onScrollProgress?.invoke(p)
								onSelected?.invoke(i)
							}
						)
				)
			}
		}
	}
}
