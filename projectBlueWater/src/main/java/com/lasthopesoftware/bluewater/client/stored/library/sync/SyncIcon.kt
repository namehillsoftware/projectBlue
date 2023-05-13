package com.lasthopesoftware.bluewater.client.stored.library.sync

import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.shared.android.ui.theme.LocalControlColor

@Composable
fun SyncIcon(
	isActive: Boolean,
	modifier: Modifier = Modifier,
	contentDescription: String = stringResource(id = R.string.btn_sync_item)
) {
	Icon(
		painter = painterResource(id = R.drawable.ic_sync_36dp),
		contentDescription = contentDescription,
		tint = if (isActive) MaterialTheme.colors.primary else LocalControlColor.current,
		modifier = modifier,
	)
}
