package com.lasthopesoftware.bluewater.android.ui.theme

import android.view.Window
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lasthopesoftware.bluewater.shared.android.ui.findWindow

val LocalControlColor = compositionLocalOf { Color.Black }
val LocalSurfaceColor = compositionLocalOf { Color.White }

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
		CompositionLocalProvider(
			LocalControlColor provides controlColor,
			LocalSurfaceColor provides color,
			content = content)
	}
}

@Composable
fun DetermineWindowControlColors() {
	findWindow()?.isStatusBarLight = LocalSurfaceColor.current.luminance() > .5f
}

var Window.isStatusBarLight: Boolean
	get() = WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars
	set(value) {
		WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = value
	}
