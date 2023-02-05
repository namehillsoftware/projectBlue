package com.lasthopesoftware.bluewater.client.stored.library.sync

import androidx.compose.foundation.Image
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R

@Composable
fun SyncButton(
	isActive: Boolean,
	modifier: Modifier = Modifier,
) {
	Image(
		painter = painterResource(id = R.drawable.ic_sync_white),
		contentDescription = stringResource(id = R.string.btn_sync_item),
		colorFilter = ColorFilter.tint(if (isActive) MaterialTheme.colors.primary else LocalContentColor.current),
		modifier = modifier,
	)
}
