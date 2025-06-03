package com.lasthopesoftware.bluewater.android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalControlColor = compositionLocalOf { Color.Black }

@Composable
fun ControlSurface(
	modifier: Modifier = Modifier,
	shape: Shape = RectangleShape,
	color: Color = MaterialTheme.colors.surface,
	contentColor: Color = contentColorFor(color),
	controlColor: Color = contentColor.copy(alpha = SharedAlphas.controlAlpha),
	border: BorderStroke? = null,
	elevation: Dp = 0.dp,
	content: @Composable () -> Unit
) {
	Surface(
		modifier = modifier,
		shape = shape,
		color = color,
		contentColor = contentColor,
		border = border,
		elevation = elevation
	) {
		CompositionLocalProvider(LocalControlColor provides controlColor, content = content)
	}
}
