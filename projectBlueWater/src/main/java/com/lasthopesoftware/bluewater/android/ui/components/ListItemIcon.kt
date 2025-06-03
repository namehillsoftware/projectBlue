package com.lasthopesoftware.bluewater.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.lasthopesoftware.bluewater.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.android.ui.theme.LocalControlColor

/**
 * An icon component that draws [painter] using [tint], with a default value
 * of [LocalContentColor]. The Icon has a default size of [Dimensions.listItemMenuIconSize]. Icon is an opinionated
 * component designed to be used with single-color icons so that they can be tinted correctly for the component they are
 * placed in. For multicolored icons and icons that should not be tinted, use [Color.Unspecified] for [tint]. For generic
 * images that should not be tinted, and do not follow the recommended icon size, use the generic
 * [androidx.compose.foundation.Image] instead. For a clickable icon, see [IconButton].
 *
 * @param painter [Painter] to draw inside this Icon
 * @param contentDescription text used by accessibility services to describe what this icon
 * represents. This should always be provided unless this icon is used for decorative purposes,
 * and does not represent a meaningful action that a user can take. This text should be
 * localized, such as by using [androidx.compose.ui.res.stringResource] or similar
 * @param modifier optional [Modifier] for this Icon
 * @param tint tint to be applied to [painter]. If [Color.Unspecified] is provided, then no tint is
 * applied
 */
@Composable
fun ListItemIcon(
	painter: Painter,
	contentDescription: String,
	modifier: Modifier = Modifier,
	tint: Color = LocalControlColor.current,
) {
	Icon(
		painter = painter,
		contentDescription = contentDescription,
		modifier = Modifier.size(Dimensions.listItemMenuIconSize).then(modifier),
		tint = tint
	)
}
