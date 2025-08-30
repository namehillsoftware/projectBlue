package com.lasthopesoftware.bluewater.android.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun AnchoredChips(
	modifier: Modifier = Modifier,
	anchoredScrollConnectionState: AnchoredScrollConnectionState,
	lazyListState: LazyListState,
	chipLabel: @Composable (Int, Float) -> Unit,
	onScrollProgress: ((Float) -> Unit)? = null,
	onSelected: ((Int) -> Unit)? = null
) {
	val targetAlpha =
		if (lazyListState.isScrollInProgress) {
			1f
		} else {
			0f
		}
	val animationDurationMs =
		if (lazyListState.isScrollInProgress) {
			150
		} else {
			500
		}
	val animationDelayMs =
		if (lazyListState.isScrollInProgress) {
			0
		} else {
			3000
		}

	val alpha by animateFloatAsState(
		targetValue = targetAlpha,
		animationSpec = tween(delayMillis = animationDelayMs, durationMillis = animationDurationMs)
	)

	if (alpha <= 0f) return

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

	Column(
		modifier = modifier
			.fillMaxHeight()
			.alpha(alpha),
		verticalArrangement = Arrangement.SpaceBetween,
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		for ((i, p) in anchoredPercentages) {
			Chip(
				onClick = {
					onScrollProgress?.invoke(p)
					onSelected?.invoke(i)
				},
				border = ChipDefaults.outlinedBorder,
				colors = ChipDefaults.chipColors(),
				modifier = Modifier.padding(viewPaddingUnit),
			) {
				chipLabel(i, p)
			}
		}
	}
}
