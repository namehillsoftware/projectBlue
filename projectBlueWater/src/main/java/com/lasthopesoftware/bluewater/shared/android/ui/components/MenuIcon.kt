package com.lasthopesoftware.bluewater.shared.android.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.lasthopesoftware.bluewater.shared.android.ui.navigable
import com.lasthopesoftware.bluewater.shared.android.ui.theme.Dimensions
import com.lasthopesoftware.bluewater.shared.android.ui.theme.LocalControlColor

@Composable
fun RowScope.ColumnMenuIcon(
	onClick: () -> Unit,
	iconPainter: Painter,
	contentDescription: String,
	modifier: Modifier = Modifier,
	label: String? = null,
	labelColor: Color = LocalContentColor.current,
	labelModifier: Modifier = Modifier,
	labelMaxLines: Int = 1,
	enabled: Boolean = true,
) {
	MenuIcon(
		onClick = onClick,
		icon = {
			Icon(
				painter = iconPainter,
				contentDescription = contentDescription,
				modifier = Modifier.size(Dimensions.topMenuIconSize),
				tint = if (enabled) LocalControlColor.current else LocalControlColor.current.copy(alpha = .6f)
			)
		},
		modifier = Modifier
			.weight(1f)
			.then(modifier),
		label = label,
		labelColor = labelColor,
		labelModifier = labelModifier,
		labelMaxLines = labelMaxLines,
		enabled = enabled
	)
}

@Composable
fun RowScope.ColumnMenuIcon(
	onClick: () -> Unit,
	icon: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	label: String? = null,
	labelColor: Color = LocalContentColor.current,
	labelModifier: Modifier = Modifier,
	labelMaxLines: Int = 1,
	enabled: Boolean = true
) {
	MenuIcon(
		onClick = onClick,
		icon = icon,
		modifier = Modifier
			.weight(1f)
			.then(modifier),
		label = label,
		labelColor = labelColor,
		labelModifier = labelModifier,
		labelMaxLines = labelMaxLines,
		enabled = enabled
	)
}

@Composable
fun MenuIcon(
	onClick: () -> Unit,
	icon: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	label: String? = null,
	labelColor: Color = LocalContentColor.current,
	labelModifier: Modifier = Modifier,
	labelMaxLines: Int = Int.MAX_VALUE,
	enabled: Boolean = true,
) {
	if (label != null) {
		MenuIcon(
			onClick = onClick,
			icon = icon,
			modifier = modifier,
			label = {
				Text(
					text = label,
					modifier = labelModifier,
					maxLines = labelMaxLines,
					color = labelColor,
				)
			},
			enabled = enabled
		)
	} else {
		MenuIcon(
			onClick = onClick,
			icon = icon,
			modifier = modifier,
			label = null as @Composable (() -> Unit)?,
			enabled = enabled
		)
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuIcon(
	onClick: () -> Unit,
	icon: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	label: @Composable (() -> Unit)? = null,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	enabled: Boolean = true,
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.navigable(
				interactionSource = interactionSource,
				indication = ripple(),
				onClick = onClick,
				enabled = enabled,
			)
			.then(modifier)
	) {
		icon()

		label?.invoke()
	}
}
