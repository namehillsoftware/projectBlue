package com.lasthopesoftware.bluewater.android.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.navigable
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor
import kotlin.math.abs
import kotlin.math.round

private val anchorSize = viewPaddingUnit * 2
private val minAnchorVerticalPadding = viewPaddingUnit
private val totalAnchorSize = anchorSize + minAnchorVerticalPadding * 2
private val dragIconSize = Dimensions.listItemMenuIconSize

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

		val maxHeightPx = LocalDensity.current.remember { maxHeight.toPx() }

		var draggedPosition by remember { mutableFloatStateOf(0f) }
		val progress by remember { derivedStateOf { (draggedPosition / maxHeightPx).coerceIn(0f, 1f) } }
		Image(
			painter = painterResource(id = R.drawable.drag),
			contentDescription = stringResource(id = R.string.drag_item),
			modifier = Modifier
				.size(dragIconSize)
				.offset { IntOffset(x = 0, y = ((progress * maxHeightPx) - dragIconSize.toPx() * .5).fastRoundToInt()) }
				.background(LocalControlColor.current, RoundedCornerShape(1.dp))
				.padding(viewPaddingUnit),
		)

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
						val roundedProgress = round(progress * 100) / 100
						val selectedAnchor = anchoredPercentages.firstOrNull { (_, p) -> p == roundedProgress }
						if (selectedAnchor == null) {
							onScrollProgress?.invoke(roundedProgress)
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
								draggedPosition = maxHeightPx * p
								onScrollProgress?.invoke(p)
								onSelected?.invoke(i)
							}
						)
				)
			}
		}
	}
}
