package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ListMenuRow(
	modifier: Modifier = Modifier,
	verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
	content: @Composable RowScope.() -> Unit,
) {
	Row(
		modifier = Modifier
			.focusGroup()
			.then(modifier)
			.horizontalScroll(rememberScrollState())
			.wrapContentHeight(),
		horizontalArrangement = Arrangement.Start,
		verticalAlignment = verticalAlignment,
	) {
		content()
	}
}
