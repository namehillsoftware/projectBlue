package com.lasthopesoftware.bluewater.client.stored.library.sync

import androidx.compose.foundation.Image
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R

@Composable
fun SyncIcon(
	isActive: Boolean,
	modifier: Modifier = Modifier,
	contentDescription: String = stringResource(id = R.string.btn_sync_item)
) {

	Image(
		painter = painterResource(id = R.drawable.ic_sync_36dp),
		contentDescription = contentDescription,
		colorFilter = if (isActive) ColorFilter.tint(MaterialTheme.colors.primary) else null,
		modifier = modifier,
	)
}
