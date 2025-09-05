package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.rowPadding
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuHeight
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions.topMenuIconSize

@Composable
fun ListMenuRow(
	modifier: Modifier = Modifier,
	content: @Composable RowScope.() -> Unit,
) {
	Row(
		modifier = Modifier
			.height(topMenuHeight)
			.padding(vertical = rowPadding, horizontal = topMenuIconSize)
			.focusGroup()
			.then(modifier)
			.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.spacedBy(topMenuIconSize * 2),
	) {
		content()
	}
}
