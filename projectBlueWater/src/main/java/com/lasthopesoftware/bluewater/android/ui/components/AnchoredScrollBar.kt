package com.lasthopesoftware.bluewater.android.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit
import kotlin.math.abs

private val dragIconSize = Dimensions.listItemMenuIconSize
private val anchorIconSize = viewPaddingUnit * 4

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AnchoredScrollBar(
	modifier: Modifier = Modifier,
	anchoredScrollConnectionState: AnchoredScrollConnectionState,
	lazyListState: LazyListState,
	onScrollProgress: ((Float) -> Unit)? = null,
	onSelected: ((Int) -> Unit)? = null
) {
	var isDragInProgress by remember { mutableStateOf(false) }

	val targetAlpha =
		if (lazyListState.isScrollInProgress || isDragInProgress) {
			1f
		} else {
			0f
		}
	val animationDurationMs =
		if (lazyListState.isScrollInProgress || isDragInProgress) {
			150
		} else {
			500
		}
	val animationDelayMs =
		if (lazyListState.isScrollInProgress || isDragInProgress) {
			0
		} else {
			3000
		}

	val alpha by animateFloatAsState(
		targetValue = targetAlpha,
		animationSpec = tween(delayMillis = animationDelayMs, durationMillis = animationDurationMs)
	)

	if (alpha > 0f) {
		BoxWithConstraints(
			modifier = modifier
				.fillMaxHeight()
				.requiredWidth(anchorIconSize + dragIconSize + viewPaddingUnit * 2)
				.padding(vertical = dragIconSize / 2)
				.alpha(alpha),
			contentAlignment = Alignment.TopCenter,
		) {
			val anchoredPercentages = remember(anchoredScrollConnectionState.progressAnchors) {
				val boundedAnchors = anchoredScrollConnectionState.progressAnchors.distinct().sorted()

				val separatedAnchors = boundedAnchors
					.filter { a ->
						for (b in boundedAnchors) {
							if (b == a) continue
							if (b > a) return@filter true
							if (a - b < .025) return@filter false
						}

						true
					}
					.withIndex()

				separatedAnchors
			}

			val maxHeightPx = LocalDensity.current.remember(maxHeight) { maxHeight.toPx() }

			var draggedPosition by remember(maxHeightPx) {
				mutableFloatStateOf(anchoredScrollConnectionState.progress * maxHeightPx)
			}
			Image(
				painter = painterResource(id = R.drawable.drag),
				contentDescription = stringResource(id = R.string.drag_item),
				colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary),
				modifier = Modifier
					.size(dragIconSize)
					.align(Alignment.TopEnd)
					.offset {
						IntOffset(
							x = 0,
							y = ((anchoredScrollConnectionState.progress * maxHeight.toPx()) - dragIconSize.toPx() * .5).fastRoundToInt()
						)
					}
					.background(MaterialTheme.colors.secondary, RoundedCornerShape(4.dp))
					.pointerInput(Unit) {
						detectVerticalDragGestures(
							onDragStart = { _ -> isDragInProgress = true },
							onDragEnd = { isDragInProgress = false },
							onDragCancel = { isDragInProgress = false }
						) { change, dragAmount ->
							change.consume()
							val maxHeightPx = maxHeight.toPx()
							draggedPosition = (draggedPosition + dragAmount).fastCoerceIn(0f, maxHeightPx)
							val progress = draggedPosition / maxHeightPx
							val direction = dragAmount / abs(dragAmount)
							val selectedAnchor = anchoredPercentages
								.filter { (_, p) ->
									val difference = direction * (p - progress)
									difference < .015f && difference >= 0f
								}
								.minByOrNull { (_, p) -> direction * (p - progress) }

							if (selectedAnchor == null) {
								onScrollProgress?.invoke(progress)
							} else {
								val (anchor, progress) = selectedAnchor
								draggedPosition = progress * maxHeightPx
								onScrollProgress?.invoke(progress)
								onSelected?.invoke(anchor)
							}
						}
					}
					.padding(viewPaddingUnit),
			)
		}
	}
}
