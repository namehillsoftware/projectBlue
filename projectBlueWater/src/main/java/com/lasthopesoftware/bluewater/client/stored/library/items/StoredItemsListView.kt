package com.lasthopesoftware.bluewater.client.stored.library.items

import VerticalHeaderScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lasthopesoftware.bluewater.android.ui.components.DeferredPreScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.FullScreenScrollConnectedScaler
import com.lasthopesoftware.bluewater.android.ui.components.ListMenuRow
import com.lasthopesoftware.bluewater.android.ui.components.ignoreConsumedOffset
import com.lasthopesoftware.bluewater.android.ui.components.linkedTo
import com.lasthopesoftware.bluewater.android.ui.remember
import com.lasthopesoftware.bluewater.android.ui.theme.ControlSurface
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.appBarHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.expandedTitleHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.viewPaddingUnit

private val boxHeight = expandedTitleHeight + appBarHeight

@Composable
private fun StoredItemsMenu(
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier
	) {
		ListMenuRow(
			modifier = Modifier.fillMaxWidth()
		) {
		}
	}
}

@Composable
fun BoxWithConstraintsScope.StoredItemsListView(

) {
	ControlSurface {
		if (maxWidth < Dimensions.twoColumnThreshold) {
			val appBarHeightPx = LocalDensity.current.remember { appBarHeight.toPx() }
			val boxHeightPx = LocalDensity.current.remember { boxHeight.toPx() }
			val heightScaler = FullScreenScrollConnectedScaler.remember(min = appBarHeightPx, max = boxHeightPx)
			val topMenuHeightPx =
				LocalDensity.current.remember { topMenuHeight.toPx() + (24.dp + viewPaddingUnit * 2).toPx() }
			val menuHeightScaler = DeferredPreScrollConnectedScaler.remember(topMenuHeightPx, 0f)
			val compositeScroller = remember(heightScaler, menuHeightScaler) {
				heightScaler.linkedTo(menuHeightScaler).ignoreConsumedOffset()
			}

			val listFocus = remember { FocusRequester() }
			VerticalHeaderScaffold(
				header = {},
				overlay = {
					val menuHeightValue by menuHeightScaler.valueState
					val menuHeightValueDp by LocalDensity.current.remember { derivedStateOf { menuHeightValue.toDp() } }
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.background(MaterialTheme.colors.surface)
							.height(menuHeightValueDp)
							.clipToBounds()
					) {
						StoredItemsMenu(
							modifier = Modifier
								.graphicsLayer {
									translationY = (menuHeightValue - topMenuHeightPx) * 0.5f
								}
						)
					}
				},
				content = { headerHeight ->

				},
				modifier = Modifier
					.fillMaxSize()
					.nestedScroll(compositeScroller)
			)
		}
	}
}
