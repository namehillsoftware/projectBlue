package com.lasthopesoftware.bluewater.android.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
object Dimensions {
	val standardRowHeight = 60.dp
	val twoLineRowHeight = 64.dp
	val appBarHeight = 56.dp
	val topMenuIconSize = 24.dp
	val listItemMenuIconSize = 36.dp
	val expandedTitleHeight = 84.dp
	val viewPaddingUnit = 4.dp
	val topRowOuterPadding by lazy(LazyThreadSafetyMode.NONE) { viewPaddingUnit * 4 }
	val menuHeight by lazy(LazyThreadSafetyMode.NONE) { topMenuIconSize + viewPaddingUnit * 6 }
	val topMenuIconSizeWithPadding by lazy(LazyThreadSafetyMode.NONE) { topMenuIconSize + topRowOuterPadding * 2 }
	val rowPadding by lazy(LazyThreadSafetyMode.NONE) { viewPaddingUnit * 2 }
	val rowPaddingValues by lazy(LazyThreadSafetyMode.NONE) { PaddingValues(rowPadding) }
	val mediumWindowWidth = 600.dp
	val twoColumnThreshold = mediumWindowWidth
	val Dp.rowScrollPadding: Rect
		get() = Rect(
			left = 0f,
			right = 0f,
			top = value * 3,
			bottom = value * 3
		)
}
