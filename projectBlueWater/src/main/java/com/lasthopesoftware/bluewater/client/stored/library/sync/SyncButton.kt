package com.lasthopesoftware.bluewater.client.stored.library.sync

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lasthopesoftware.bluewater.R

@Composable
fun SyncButton(
	isActive: Boolean,
	modifier: Modifier = Modifier,
) {
	fun getContentControlColor(context: Context, defaultColor: Color): Color {
		val contentControlAttribute = context.obtainStyledAttributes(R.style.AppTheme, intArrayOf(R.attr.colorControlNormal))
		try {
			val color = contentControlAttribute.getColor(0, defaultColor.value.toInt())
			return Color(color)
		} finally {
		    contentControlAttribute.recycle()
		}
	}

	Image(
		painter = painterResource(id = R.drawable.ic_sync_white),
		contentDescription = stringResource(id = R.string.btn_sync_item),
		colorFilter = ColorFilter.tint(
			if (isActive) MaterialTheme.colors.primary
			else getContentControlColor(LocalContext.current, LocalContentColor.current)),
		modifier = modifier,
	)
}
