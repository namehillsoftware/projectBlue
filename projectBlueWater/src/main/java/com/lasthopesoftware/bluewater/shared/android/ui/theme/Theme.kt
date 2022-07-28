package com.lasthopesoftware.bluewater.shared.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
	primary = Dark.ProjectBlue,
	onPrimary = Color.White,
	primaryVariant = Dark.ProjectBlueLight,
	secondary = Dark.ProjectBlueDark,
	onSecondary = Color.White,
)

private val LightColorPalette = lightColors(
	primary = Light.ProjectBlue,
	onPrimary = Color.White,
	primaryVariant = Light.ProjectBlueLight,
	secondary = Light.ProjectBlueDark,
	onSecondary = Color.White,
	surface = Color.White,
	onSurface = Color.Black,

	/* Other default colors to override
	background = Color.White,
	onPrimary = Color.White,
	onSecondary = Color.Black,
	onBackground = Color.Black,
	*/
)

@Composable
fun ProjectBlueTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colors = if (darkTheme) DarkColorPalette else LightColorPalette

	MaterialTheme(
		colors = colors,
		typography = Typography,
		shapes = Shapes,
		content = content
	)
}
