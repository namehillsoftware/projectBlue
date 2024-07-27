package com.lasthopesoftware.bluewater.shared.android.ui.theme

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
	val menuHeight = 48.dp
	val expandedTitleHeight = 84.dp
	val viewPaddingUnit = 4.dp
	val topRowOuterPadding = viewPaddingUnit * 4
	val rowPaddingValues = PaddingValues(viewPaddingUnit * 2)
	val mediumWindowWidth = 600.dp
	val twoColumnThreshold = mediumWindowWidth
	val expandedMenuVerticalPadding = viewPaddingUnit * 3
	val Dp.rowScrollPadding: Rect
		get() = Rect(
			left = 0f,
			right = 0f,
			top = value * 3,
			bottom = value * 3
		)
}
